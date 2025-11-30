package com.verumdec.core

import android.content.Context
import com.verumdec.core.brain.*
import com.verumdec.core.constitution.ConstitutionBrain
import com.verumdec.core.storage.LocalStorageManager
import com.verumdec.core.util.*

/**
 * Core Module - Central API for Verumdec Engine
 *
 * This module provides the foundational components for the Verumdec offline contradiction engine.
 * It contains shared data models, utilities, brains, and base interfaces used across all modules.
 *
 * ## Key Components:
 * - Brain classes for document, image, audio, video, email, signature, behavioral analysis
 * - Constitution enforcement system
 * - Local storage manager
 * - Utility classes for hashing, dates, files, etc.
 *
 * ## Usage:
 * ```kotlin
 * CoreModule.initialize(context)
 * val documentBrain = CoreModule.getDocumentBrain()
 * val result = documentBrain.analyze(content, fileName)
 * ```
 */
object CoreModule {

    const val VERSION = "1.0.0"
    const val NAME = "core"

    private var isInitialized = false
    private var appContext: Context? = null
    
    // Lazy-initialized brains
    private var documentBrain: DocumentBrain? = null
    private var imageBrain: ImageBrain? = null
    private var audioBrain: AudioBrain? = null
    private var videoBrain: VideoBrain? = null
    private var emailBrain: EmailBrain? = null
    private var signatureBrain: SignatureBrain? = null
    private var behavioralBrain: BehavioralBrain? = null
    private var timelineBrain: TimelineBrain? = null
    private var pdfBrain: PDFBrain? = null
    private var storageManager: LocalStorageManager? = null

    /**
     * Initialize the core module with application context.
     */
    fun initialize(context: Context) {
        if (isInitialized) return
        
        appContext = context.applicationContext
        
        // Initialize constitution
        ConstitutionBrain.getInstance(context).loadConstitution()
        
        isInitialized = true
    }

    /**
     * Check if module is initialized.
     */
    fun isInitialized(): Boolean = isInitialized

    /**
     * Get application context.
     */
    fun getContext(): Context {
        return appContext ?: throw IllegalStateException("CoreModule not initialized. Call initialize() first.")
    }

    // Brain getters

    fun getDocumentBrain(): DocumentBrain {
        val ctx = getContext()
        return documentBrain ?: DocumentBrain(ctx).also { documentBrain = it }
    }

    fun getImageBrain(): ImageBrain {
        val ctx = getContext()
        return imageBrain ?: ImageBrain(ctx).also { imageBrain = it }
    }

    fun getAudioBrain(): AudioBrain {
        val ctx = getContext()
        return audioBrain ?: AudioBrain(ctx).also { audioBrain = it }
    }

    fun getVideoBrain(): VideoBrain {
        val ctx = getContext()
        return videoBrain ?: VideoBrain(ctx).also { videoBrain = it }
    }

    fun getEmailBrain(): EmailBrain {
        val ctx = getContext()
        return emailBrain ?: EmailBrain(ctx).also { emailBrain = it }
    }

    fun getSignatureBrain(): SignatureBrain {
        val ctx = getContext()
        return signatureBrain ?: SignatureBrain(ctx).also { signatureBrain = it }
    }

    fun getBehavioralBrain(): BehavioralBrain {
        val ctx = getContext()
        return behavioralBrain ?: BehavioralBrain(ctx).also { behavioralBrain = it }
    }

    fun getTimelineBrain(): TimelineBrain {
        val ctx = getContext()
        return timelineBrain ?: TimelineBrain(ctx).also { timelineBrain = it }
    }

    fun getPDFBrain(): PDFBrain {
        val ctx = getContext()
        return pdfBrain ?: PDFBrain(ctx).also { pdfBrain = it }
    }

    fun getConstitutionBrain(): ConstitutionBrain {
        return ConstitutionBrain.getInstance(getContext())
    }

    fun getStorageManager(): LocalStorageManager {
        val ctx = getContext()
        return storageManager ?: LocalStorageManager(ctx).also { storageManager = it }
    }

    // Utility class access

    object Utils {
        val hash = HashUtils
        val date = DateUtils
        val file = FileUtils
        val mime = MimeUtils
        val metadata = MetadataUtils
        val stream = StreamUtils
        val safeWrite = SafeWriteUtils
        val image = ImageUtils
        val audio = AudioUtils
        val video = VideoUtils
        val document = DocumentUtils
    }

    /**
     * Reset the module (for testing).
     */
    fun reset() {
        documentBrain = null
        imageBrain = null
        audioBrain = null
        videoBrain = null
        emailBrain = null
        signatureBrain = null
        behavioralBrain = null
        timelineBrain = null
        pdfBrain = null
        storageManager = null
        isInitialized = false
        appContext = null
    }
}
