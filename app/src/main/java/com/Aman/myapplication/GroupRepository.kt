// Updated GroupRepository.kt - Fixed for users table with corrected query syntax

package com.Aman.myapplication

import android.util.Log
import androidx.annotation.RequiresApi
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.Serializable

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
    val updated_at: String? = null,
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
                Log.d("GroupRepository", "Creating group: name=$name, description=$description, userId=$userId")

                // Create a serializable data class for the group creation
                @Serializable
                data class CreateGroupRequest(
                    val name: String,
                    val description: String? = null,
                    val created_by: String
                )

                val groupData = CreateGroupRequest(
                    name = name,
                    description = description,
                    created_by = userId
                )

                Log.d("GroupRepository", "Inserting group with data: $groupData")

                val group = supabase.from("groups").insert(groupData) {
                    select()
                }.decodeSingle<Group>()

                Log.d("GroupRepository", "Group created successfully: ${group.id}")

                // Add creator as admin member
                try {
                    @Serializable
                    data class CreateMemberRequest(
                        val group_id: String,
                        val user_id: String,
                        val role: String
                    )

                    val memberData = CreateMemberRequest(
                        group_id = group.id,
                        user_id = userId,
                        role = "admin"
                    )

                    Log.d("GroupRepository", "Adding creator as admin: $memberData")

                    supabase.from("group_members").insert(memberData)

                    Log.d("GroupRepository", "Creator added as admin successfully")

                    Result.success(group)
                } catch (memberError: Exception) {
                    Log.e("GroupRepository", "Failed to add creator as member: ${memberError.message}", memberError)

                    // Try to clean up the group if member addition fails
                    try {
                        supabase.from("groups").delete {
                            filter {
                                eq("id", group.id)
                            }
                        }
                        Log.d("GroupRepository", "Cleaned up group after member addition failure")
                    } catch (cleanupError: Exception) {
                        Log.e("GroupRepository", "Failed to cleanup group: ${cleanupError.message}")
                    }

                    Result.failure(Exception("Failed to add you as group admin: ${memberError.message}"))
                }
            } catch (e: Exception) {
                Log.e("GroupRepository", "Failed to create group: ${e.message}", e)
                Result.failure(Exception("Failed to create group: ${e.message}"))
            }
        }
    }

    // Get all groups for a user - Simplified approach
    suspend fun getUserGroups(userId: String): Result<List<Group>> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("GroupRepository", "Getting groups for user: $userId")

                // Get group IDs from group_members first
                val memberGroups = supabase.from("group_members")
                    .select(Columns.list("group_id")) {
                        filter {
                            eq("user_id", userId)
                        }
                    }
                    .decodeList<Map<String, String>>()

                Log.d("GroupRepository", "User is member of ${memberGroups.size} groups")

                if (memberGroups.isEmpty()) {
                    return@withContext Result.success(emptyList())
                }

                val groupIds = memberGroups.map { it["group_id"]!! }

                // Get the actual group details
                val groups = supabase.from("groups")
                    .select() {
                        filter {
                            isIn("id", groupIds)
                        }
                    }
                    .decodeList<Group>()

                Log.d("GroupRepository", "Retrieved ${groups.size} group details")
                Result.success(groups)

            } catch (e: Exception) {
                Log.e("GroupRepository", "Error getting user groups: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    // Get group members - Fixed query syntax
    suspend fun getGroupMembers(groupId: String): Result<List<GroupMember>> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("GroupRepository", "Getting members for group: $groupId")

                // First get the group members
                val members = supabase.from("group_members")
                    .select() {
                        filter {
                            eq("group_id", groupId)
                        }
                    }
                    .decodeList<GroupMember>()

                // Then get user details for each member
                val membersWithUsers = members.map { member ->
                    try {
                        val user = supabase.from("users")
                            .select() {
                                filter {
                                    eq("id", member.user_id)
                                }
                            }
                            .decodeSingleOrNull<User>()

                        member.copy(user = user)
                    } catch (e: Exception) {
                        Log.w("GroupRepository", "Failed to get user details for ${member.user_id}: ${e.message}")
                        member
                    }
                }

                Log.d("GroupRepository", "Found ${membersWithUsers.size} members")
                Result.success(membersWithUsers)
            } catch (e: Exception) {
                Log.e("GroupRepository", "Error getting group members: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    // Add member to group
    suspend fun addMemberToGroup(groupId: String, userId: String, role: String = "member"): Result<GroupMember> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("GroupRepository", "Adding member to group: groupId=$groupId, userId=$userId, role=$role")

                @Serializable
                data class AddMemberRequest(
                    val group_id: String,
                    val user_id: String,
                    val role: String
                )

                val memberData = AddMemberRequest(
                    group_id = groupId,
                    user_id = userId,
                    role = role
                )

                val member = supabase.from("group_members").insert(memberData) {
                    select()
                }.decodeSingle<GroupMember>()

                Log.d("GroupRepository", "Member added successfully")
                Result.success(member)
            } catch (e: Exception) {
                Log.e("GroupRepository", "Error adding member to group: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    // Search users - Fixed query
    suspend fun searchUsers(query: String): Result<List<User>> = withContext(Dispatchers.IO) {
        try {
            if (query.isBlank()) return@withContext Result.success(emptyList())

            Log.d("GroupRepository", "Searching users with query: $query")

            val users = supabase.from("users")
                .select() {
                    filter {
                        or {
                            ilike("email", "%$query%")
                            ilike("full_name", "%$query%")
                        }
                    }
                }
                .decodeList<User>()

            Log.d("GroupRepository", "Found ${users.size} users")
            Result.success(users)
        } catch (e: Exception) {
            Log.e("GroupRepository", "Error searching users: ${e.message}", e)
            Result.failure(e)
        }
    }

    // Get a specific group by ID
    suspend fun getGroupById(groupId: String): Result<Group> {
        return withContext(Dispatchers.IO) {
            try {
                val group = supabase.from("groups")
                    .select() {
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
                @Serializable
                data class UpdateGroupRequest(
                    val name: String,
                    val description: String?
                )

                val updateData = UpdateGroupRequest(
                    name = name,
                    description = description
                )

                val group = supabase.from("groups")
                    .update(updateData) {
                        filter {
                            eq("id", groupId)
                        }
                        select()
                    }
                    .decodeSingle<Group>()

                Result.success(group)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // Get user friends - Fixed query syntax
    suspend fun getUserFriends(userId: String): Result<List<Friend>> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("GroupRepository", "Getting friends for user: $userId")

                // First get the friend relationships
                val friendships = supabase.from("friends")
                    .select() {
                        filter {
                            eq("user_id", userId)
                            eq("status", "accepted")
                        }
                    }
                    .decodeList<Friend>()

                Log.d("GroupRepository", "Found ${friendships.size} friend relationships")

                // Then get user details for each friend
                val friendsWithDetails = friendships.map { friendship ->
                    try {
                        val friendUser = supabase.from("users")
                            .select() {
                                filter {
                                    eq("id", friendship.friend_id)
                                }
                            }
                            .decodeSingleOrNull<User>()

                        friendship.copy(friend = friendUser)
                    } catch (e: Exception) {
                        Log.w("GroupRepository", "Failed to get friend details for ${friendship.friend_id}: ${e.message}")
                        friendship
                    }
                }

                Log.d("GroupRepository", "Loaded friend details for ${friendsWithDetails.size} friends")
                Result.success(friendsWithDetails)
            } catch (e: Exception) {
                Log.e("GroupRepository", "Error getting user friends: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    // Send friend request
    suspend fun sendFriendRequest(userId: String, friendEmail: String): Result<Friend> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("GroupRepository", "Sending friend request from $userId to $friendEmail")

                // First find the user by email
                val friendUser = supabase.from("users")
                    .select() {
                        filter {
                            eq("email", friendEmail)
                        }
                    }
                    .decodeSingleOrNull<User>()

                if (friendUser == null) {
                    throw Exception("User with email $friendEmail not found")
                }

                @Serializable
                data class FriendRequestData(
                    val user_id: String,
                    val friend_id: String,
                    val status: String
                )

                val friendRequestData = FriendRequestData(
                    user_id = userId,
                    friend_id = friendUser.id,
                    status = "pending"
                )

                val friend = supabase.from("friends").insert(friendRequestData) {
                    select()
                }.decodeSingle<Friend>()

                Log.d("GroupRepository", "Friend request sent successfully")
                Result.success(friend)
            } catch (e: Exception) {
                Log.e("GroupRepository", "Error sending friend request: ${e.message}", e)
                Result.failure(e)
            }
        }
    }
}