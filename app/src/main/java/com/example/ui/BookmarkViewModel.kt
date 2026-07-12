package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.Bookmark
import com.example.data.BookmarkDatabase
import com.example.data.BookmarkRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BookmarkViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: BookmarkRepository

    val allBookmarks: StateFlow<List<Bookmark>>

    init {
        val database = BookmarkDatabase.getDatabase(application)
        val bookmarkDao = database.bookmarkDao()
        repository = BookmarkRepository(bookmarkDao)
        allBookmarks = repository.allBookmarks.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    fun addBookmark(title: String, url: String) {
        val formattedUrl = BookmarkUtils.formatUrl(url)
        val finalTitle = title.trim().ifEmpty { BookmarkUtils.getCleanName(formattedUrl) }
        viewModelScope.launch {
            repository.insert(Bookmark(title = finalTitle, url = formattedUrl))
        }
    }

    fun deleteBookmark(id: Int) {
        viewModelScope.launch {
            repository.delete(id)
        }
    }

    fun updateBookmark(id: Int, title: String, url: String) {
        val formattedUrl = BookmarkUtils.formatUrl(url)
        val finalTitle = title.trim().ifEmpty { BookmarkUtils.getCleanName(formattedUrl) }
        viewModelScope.launch {
            repository.update(Bookmark(id = id, title = finalTitle, url = formattedUrl))
        }
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(BookmarkViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return BookmarkViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
