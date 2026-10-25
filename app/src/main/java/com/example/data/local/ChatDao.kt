package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.MessageStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Query("SELECT * FROM conversations ORDER BY isPinned DESC, lastMessageTime DESC")
    fun getAllConversations(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE id = :conversationId LIMIT 1")
    suspend fun getConversationById(conversationId: String): ConversationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: ConversationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversations(conversations: List<ConversationEntity>)

    @Update
    suspend fun updateConversation(conversation: ConversationEntity)

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    fun getMessagesForConversation(conversationId: String): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<MessageEntity>)

    @Query("UPDATE messages SET status = :status WHERE id = :messageId")
    suspend fun updateMessageStatus(messageId: String, status: MessageStatus)

    @Query("UPDATE conversations SET lastMessageText = :text, lastMessageTime = :time WHERE id = :conversationId")
    suspend fun updateLastMessage(conversationId: String, text: String, time: Long)

    @Query("UPDATE conversations SET unreadCount = 0 WHERE id = :conversationId")
    suspend fun markConversationRead(conversationId: String)

    @Query("SELECT * FROM status_stories ORDER BY timestamp DESC")
    fun getAllStories(): Flow<List<StatusStoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStory(story: StatusStoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStories(stories: List<StatusStoryEntity>)

    @Query("UPDATE status_stories SET isViewed = 1 WHERE id = :storyId")
    suspend fun markStoryViewed(storyId: String)

    @Query("UPDATE status_stories SET viewCount = viewCount + 1 WHERE id = :storyId")
    suspend fun incrementStoryViewCount(storyId: String)

    @Query("SELECT * FROM sticker_packs")
    fun getAllStickerPacks(): Flow<List<StickerPackEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStickerPacks(packs: List<StickerPackEntity>)

    @Query("UPDATE sticker_packs SET isInstalled = :isInstalled WHERE id = :packId")
    suspend fun setStickerPackInstalled(packId: String, isInstalled: Boolean)

    // 2GB Heavy-Duty Chunked File Transfer Queries
    @Query("SELECT * FROM file_transfers ORDER BY timestamp DESC")
    fun getAllFileTransfers(): Flow<List<FileTransferEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFileTransfer(transfer: FileTransferEntity)

    @Query("UPDATE file_transfers SET uploadedChunks = :uploadedChunks, isCompleted = :isCompleted WHERE id = :transferId")
    suspend fun updateFileTransferProgress(transferId: String, uploadedChunks: Int, isCompleted: Boolean)
}
