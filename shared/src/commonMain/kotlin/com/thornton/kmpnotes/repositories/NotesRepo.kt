package com.thornton.kmpnotes.repositories

import com.thornton.kmpnotes.models.Note
import kotlinx.coroutines.flow.Flow

interface NotesRepo {
    fun getAllNotes(): Flow<List<Note>>
    suspend fun get(id: String): Note
    suspend fun create(note: Note)
    suspend fun update(note: Note)
    suspend fun delete(note: Note)
}