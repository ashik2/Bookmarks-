package com.example.data

import kotlinx.coroutines.flow.Flow

class BookmarkRepository(private val bookmarkDao: BookmarkDao) {
    val allBookmarks: Flow<List<Bookmark>> = bookmarkDao.getAllBookmarksAlphabetical()

    suspend fun insert(bookmark: Bookmark) {
        val existing = bookmarkDao.getBookmarkByUrl(bookmark.url)
        if (existing == null) {
            bookmarkDao.insertBookmark(bookmark)
        }
    }

    suspend fun update(bookmark: Bookmark) {
        bookmarkDao.insertBookmark(bookmark)
    }

    suspend fun delete(id: Int) {
        bookmarkDao.deleteBookmark(id)
    }
}
