// ContactRepository.kt
package com.Aman.myapplication

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

@Serializable
data class Contact(
    val id: String,
    val name: String,
    val phoneNumber: String,
    val email: String? = null,
    val isAppUser: Boolean = false,
    val userId: String? = null
)

data class InvitationData(
    val contact: Contact,
    val groupName: String,
    val inviterName: String
)

class ContactRepository(private val context: Context) {

    suspend fun getContacts(): Result<List<Contact>> = withContext(Dispatchers.IO) {
        try {
            if (!hasContactsPermission()) {
                return@withContext Result.failure(SecurityException("Contacts permission not granted"))
            }

            val contacts = mutableListOf<Contact>()
            val contentResolver = context.contentResolver

            val projection = arrayOf(
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Email.ADDRESS
            )

            val cursor: Cursor? = contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                null,
                null,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
            )

            cursor?.use {
                val idColumn = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                val nameColumn = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val phoneColumn = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

                while (it.moveToNext()) {
                    try {
                        val contactId = it.getString(idColumn) ?: continue
                        val name = it.getString(nameColumn) ?: "Unknown"
                        val phoneNumber = it.getString(phoneColumn) ?: continue

                        // Clean phone number
                        val cleanPhone = cleanPhoneNumber(phoneNumber)
                        if (cleanPhone.isNotEmpty()) {
                            // Get email for this contact
                            val email = getContactEmail(contentResolver, contactId)

                            val contact = Contact(
                                id = contactId,
                                name = name,
                                phoneNumber = cleanPhone,
                                email = email
                            )

                            // Avoid duplicates
                            if (contacts.none { existing -> existing.phoneNumber == cleanPhone }) {
                                contacts.add(contact)
                            }
                        }
                    } catch (e: Exception) {
                        Log.w("ContactRepository", "Error reading contact: ${e.message}")
                    }
                }
            }

            Log.d("ContactRepository", "Found ${contacts.size} contacts")
            Result.success(contacts)

        } catch (e: Exception) {
            Log.e("ContactRepository", "Error loading contacts: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun getContactEmail(contentResolver: android.content.ContentResolver, contactId: String): String? {
        return try {
            val emailCursor = contentResolver.query(
                ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                arrayOf(ContactsContract.CommonDataKinds.Email.ADDRESS),
                "${ContactsContract.CommonDataKinds.Email.CONTACT_ID} = ?",
                arrayOf(contactId),
                null
            )

            emailCursor?.use {
                if (it.moveToFirst()) {
                    val emailColumn = it.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS)
                    it.getString(emailColumn)
                } else null
            }
        } catch (e: Exception) {
            Log.w("ContactRepository", "Error getting email for contact $contactId: ${e.message}")
            null
        }
    }

    private fun cleanPhoneNumber(phoneNumber: String): String {
        // Remove all non-digit characters except +
        return phoneNumber.replace(Regex("[^+\\d]"), "")
    }

    suspend fun checkAppUsers(contacts: List<Contact>, userRepository: SupabaseAuthRepository): List<Contact> = withContext(Dispatchers.IO) {
        try {
            val updatedContacts = mutableListOf<Contact>()

            contacts.forEach { contact ->
                try {
                    // Check if email exists in users table (if contact has email)
                    var isAppUser = false
                    var userId: String? = null

                    contact.email?.let { email ->
                        // You'll need to add this method to SupabaseAuthRepository
                        val user = userRepository.getUserByEmail(email)
                        if (user != null) {
                            isAppUser = true
                            userId = user.id
                        }
                    }

                    updatedContacts.add(
                        contact.copy(
                            isAppUser = isAppUser,
                            userId = userId
                        )
                    )
                } catch (e: Exception) {
                    Log.w("ContactRepository", "Error checking app user for ${contact.name}: ${e.message}")
                    updatedContacts.add(contact)
                }
            }

            updatedContacts
        } catch (e: Exception) {
            Log.e("ContactRepository", "Error checking app users: ${e.message}")
            contacts
        }
    }

    fun sendWhatsAppInvitation(invitationData: InvitationData): Boolean {
        return try {
            val message = buildInvitationMessage(invitationData)
            val phoneNumber = invitationData.contact.phoneNumber

            // Create WhatsApp intent
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://api.whatsapp.com/send?phone=$phoneNumber&text=${Uri.encode(message)}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            // Check if WhatsApp is installed
            if (isWhatsAppInstalled()) {
                context.startActivity(intent)
                true
            } else {
                // Fallback to SMS
                sendSMSInvitation(invitationData)
            }
        } catch (e: Exception) {
            Log.e("ContactRepository", "Error sending WhatsApp invitation: ${e.message}")
            false
        }
    }

    fun sendSMSInvitation(invitationData: InvitationData): Boolean {
        return try {
            val message = buildInvitationMessage(invitationData)
            val phoneNumber = invitationData.contact.phoneNumber

            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:$phoneNumber")
                putExtra("sms_body", message)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e("ContactRepository", "Error sending SMS invitation: ${e.message}")
            false
        }
    }

    private fun buildInvitationMessage(invitationData: InvitationData): String {
        return if (invitationData.contact.isAppUser) {
            """
            Hi ${invitationData.contact.name}! 👋
            
            ${invitationData.inviterName} has invited you to join "${invitationData.groupName}" group on ExpenseTracker! 💰
            
            Open the app to accept the invitation and start splitting expenses together! 🎉
            """.trimIndent()
        } else {
            """
            Hi ${invitationData.contact.name}! 👋
            
            ${invitationData.inviterName} has invited you to join "${invitationData.groupName}" group on ExpenseTracker! 💰
            
            Download our app to split expenses easily:
            📱 Android: https://play.google.com/store/apps/details?id=com.Aman.myapplication
            
            Join us and make expense sharing simple! 🎉
            """.trimIndent()
        }
    }

    private fun isWhatsAppInstalled(): Boolean {
        return try {
            context.packageManager.getPackageInfo("com.whatsapp", PackageManager.GET_ACTIVITIES)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun hasContactsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        const val CONTACTS_PERMISSION_REQUEST_CODE = 1001
    }
}