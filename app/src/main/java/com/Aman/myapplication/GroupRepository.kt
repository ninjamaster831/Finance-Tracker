package com.Aman.myapplication


import android.os.Build
import android.service.autofill.Validators.or
import androidx.annotation.RequiresApi
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import kotlinx.serialization.Serializable
import java.util.Locale.filter

@Serializable
data class User(
    val id: String,
    val email: String,
    val full_name: String? = null,
    val phone: String? = null,
    val avatar_url: String? = null,
    val created_at: String? = null
)
@Serializable
data class Group(
    val id: String,
    val name: String,
    val description: String? = null,
    val created_by: String,
    val created_at: String,
    val updated_at: String,
    val member_count: Int = 0
)

@Serializable
data class GroupMember(
    val id: String,
    val group_id: String,
    val user_id: String,
    val role: String = "member", // admin, member
    val joined_at: String,
    val user: User? = null
)

@Serializable
data class Friend(
    val id: String,
    val user_id: String,
    val friend_id: String,
    val status: String = "pending", // pending, accepted, blocked
    val created_at: String,
    val friend: User? = null
)
class GroupRepository(private val supabase: SupabaseClient) {

    // Create a new group
    suspend fun createGroup(name: String, description: String?, userId: String): Result<Group> {
        return withContext(Dispatchers.IO) {
            try {
                val group = supabase.from("groups").insert(
                    mapOf(
                        "name" to name,
                        "description" to description,
                        "created_by" to userId
                    )
                ).decodeSingle<Group>()

                // Add creator as admin member
                addMemberToGroup(group.id, userId, "admin")

                Result.success(group)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // Get all groups for a user
    suspend fun getUserGroups(userId: String): Result<List<Group>> {
        return withContext(Dispatchers.IO) {
            try {
                val groups = supabase.from("groups")
                    .select(
                        Columns.list("""
                        *,
                        group_members!inner(user_id)
                    """.trimIndent())){
                        filter {
                            eq("group_members.user_id", userId)
                        }
                    }

                    .decodeList<Group>()

                Result.success(groups)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // Get group members
    suspend fun getGroupMembers(groupId: String): Result<List<GroupMember>> {
        return withContext(Dispatchers.IO) {
            try {
                val members = supabase.from("group_members")
                    .select(
                        Columns.list("""
                        *,
                        users(id, email, full_name, avatar_url)
                    """.trimIndent())){
                        filter {
                            eq("group_id", groupId)
                        }
                    }

                    .decodeList<GroupMember>()

                Result.success(members)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // Add member to group
    suspend fun addMemberToGroup(groupId: String, userId: String, role: String = "member"): Result<GroupMember> {
        return withContext(Dispatchers.IO) {
            try {
                val member = supabase.from("group_members").insert(
                    mapOf(
                        "group_id" to groupId,
                        "user_id" to userId,
                        "role" to role
                    )
                ).decodeSingle<GroupMember>()

                Result.success(member)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun searchUsers(query: String): Result<List<User>> = withContext(Dispatchers.IO) {
        try {
            if (query.isBlank()) return@withContext Result.success(emptyList())

            val users = supabase.from("users")
                .select {
                    filter {
                        or {
                            ilike("email", "%$query%")
                            ilike("full_name", "%$query%")
                        }
                    }
                }
                .decodeList<User>()

            Result.success(users)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

// Add these methods to your GroupRepository class

    // Get a specific group by ID
    suspend fun getGroupById(groupId: String): Result<Group> {
        return withContext(Dispatchers.IO) {
            try {
                val group = supabase.from("groups")
                    .select(){
                        filter {
                            eq("id", groupId)
                        }
                    }

                    .decodeSingle<Group>()

                Result.success(group)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // Remove member from group
    suspend fun removeMemberFromGroup(groupId: String, userId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                supabase.from("group_members")
                    .delete {
                        filter {
                            eq("group_id", groupId)
                            eq("user_id", userId)
                        }
                    }

                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // Update group information
    suspend fun updateGroup(groupId: String, name: String, description: String?): Result<Group> {
        return withContext(Dispatchers.IO) {
            try {
                val group = supabase.from("groups")
                    .update(
                        mapOf(
                            "name" to name,
                            "description" to description
                        )
                    ) {
                        filter {
                            eq("id", groupId)
                        }
                    }
                    .decodeSingle<Group>()

                Result.success(group)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }


    // Get user friends
    suspend fun getUserFriends(userId: String): Result<List<Friend>> {
        return withContext(Dispatchers.IO) {
            try {
                val friends = supabase.from("friends")
                    .select(
                        Columns.list("""
                        *,
                        friend:users!friends_friend_id_fkey(id, email, full_name, avatar_url)
                    """.trimIndent())){
                        filter {
                            eq("user_id", userId)
                            eq("status", "accepted")
                        }
                    }

                    .decodeList<Friend>()

                Result.success(friends)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // Send friend request
    suspend fun sendFriendRequest(userId: String, friendEmail: String): Result<Friend> {
        return withContext(Dispatchers.IO) {
            try {
                // First find the user by email
                val friendUser = supabase.from("users")
                    .select(Columns.list("id")){
                        filter {
                            eq("email", friendEmail)
                        }
                    }

                    .decodeSingleOrNull<User>()
                    ?: throw Exception("User not found")

                val friend = supabase.from("friends").insert(
                    mapOf(
                        "user_id" to userId,
                        "friend_id" to friendUser.id,
                        "status" to "pending"
                    )
                ).decodeSingle<Friend>()

                Result.success(friend)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}