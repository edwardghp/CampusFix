package com.campusfix.di

import android.content.Context
import androidx.room.Room
import com.campusfix.data.local.AppDatabase
import com.campusfix.data.local.AulaDao
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.ktx.messaging
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import com.campusfix.data.local.TicketDao
import com.campusfix.data.local.ChatDao
/** Modulo Hilt: provee Firebase y la base de datos Room a toda la app. */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides @Singleton
    fun provideAuth(): FirebaseAuth = Firebase.auth

    @Provides @Singleton
    fun provideFirestore(): FirebaseFirestore = Firebase.firestore

    @Provides @Singleton
    fun provideStorage(): FirebaseStorage = Firebase.storage

    @Provides @Singleton
    fun provideMessaging(): FirebaseMessaging = Firebase.messaging

    @Provides @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "campusfix.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideAulaDao(db: AppDatabase): AulaDao = db.aulaDao()
    @Provides fun provideTicketDao(db: AppDatabase): TicketDao = db.ticketDao()
    @Provides fun provideChatDao(db: AppDatabase): ChatDao = db.chatDao()
}
