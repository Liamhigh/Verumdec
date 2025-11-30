package com.verumdec.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.util.LruCache
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.verumdec.R
import com.verumdec.databinding.ActivityForensicReportViewerBinding
import java.io.File

/**
 * Activity for viewing forensic PDF reports.
 * Uses PdfRenderer for native Android PDF rendering with lazy loading.
 */
class ForensicReportViewerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityForensicReportViewerBinding
    private var pdfRenderer: PdfRenderer? = null
    private var fileDescriptor: ParcelFileDescriptor? = null
    private var currentPage = 0
    private var totalPages = 0
    
    // LRU cache for rendered pages to avoid memory issues with large PDFs
    private val pageCache: LruCache<Int, Bitmap> = LruCache(MAX_CACHED_PAGES)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForensicReportViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupNavigation()

        val filePath = intent.getStringExtra(EXTRA_PDF_PATH)
        if (filePath != null) {
            loadPdf(File(filePath))
        } else {
            Toast.makeText(this, R.string.error_no_pdf, Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.title_report_viewer)
        binding.toolbar.setNavigationOnClickListener { onBackPressed() }
    }

    private fun setupNavigation() {
        binding.btnPrevious.setOnClickListener {
            if (currentPage > 0) {
                currentPage--
                displayPage(currentPage)
            }
        }

        binding.btnNext.setOnClickListener {
            if (currentPage < totalPages - 1) {
                currentPage++
                displayPage(currentPage)
            }
        }

        binding.btnShare.setOnClickListener {
            shareReport()
        }

        binding.btnZoomIn.setOnClickListener {
            val currentScale = binding.imageReport.scaleX
            if (currentScale < 3f) {
                binding.imageReport.scaleX = currentScale * 1.25f
                binding.imageReport.scaleY = currentScale * 1.25f
            }
        }

        binding.btnZoomOut.setOnClickListener {
            val currentScale = binding.imageReport.scaleX
            if (currentScale > 0.5f) {
                binding.imageReport.scaleX = currentScale * 0.8f
                binding.imageReport.scaleY = currentScale * 0.8f
            }
        }

        binding.btnZoomReset.setOnClickListener {
            binding.imageReport.scaleX = 1f
            binding.imageReport.scaleY = 1f
        }
    }

    private fun loadPdf(file: File) {
        try {
            if (!file.exists()) {
                Toast.makeText(this, R.string.error_file_not_found, Toast.LENGTH_SHORT).show()
                finish()
                return
            }

            binding.progressBar.visibility = View.VISIBLE

            fileDescriptor = ParcelFileDescriptor.open(
                file,
                ParcelFileDescriptor.MODE_READ_ONLY
            )

            pdfRenderer = PdfRenderer(fileDescriptor!!)
            totalPages = pdfRenderer!!.pageCount
            
            if (totalPages > 0) {
                displayPage(0)
            }

            binding.progressBar.visibility = View.GONE
            updateNavigationState()
            
            // Update title with file name
            supportActionBar?.title = file.nameWithoutExtension

        } catch (e: Exception) {
            binding.progressBar.visibility = View.GONE
            Toast.makeText(
                this,
                getString(R.string.error_loading_pdf, e.message),
                Toast.LENGTH_LONG
            ).show()
            finish()
        }
    }

    /**
     * Render a single page on demand with caching.
     * Uses LruCache to limit memory usage for large PDFs.
     */
    private fun renderPage(pageIndex: Int): Bitmap? {
        // Check cache first
        pageCache.get(pageIndex)?.let { return it }
        
        val renderer = pdfRenderer ?: return null
        if (pageIndex < 0 || pageIndex >= renderer.pageCount) return null
        
        return try {
            val page = renderer.openPage(pageIndex)
            
            // Calculate scale for good quality
            val scale = 2.0f
            val width = (page.width * scale).toInt()
            val height = (page.height * scale).toInt()
            
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            
            // White background
            bitmap.eraseColor(android.graphics.Color.WHITE)
            
            // Render the page
            page.render(
                bitmap,
                null,
                null,
                PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
            )
            
            page.close()
            
            // Cache the rendered page
            pageCache.put(pageIndex, bitmap)
            
            bitmap
        } catch (e: Exception) {
            null
        }
    }

    private fun displayPage(pageIndex: Int) {
        if (pageIndex < 0 || pageIndex >= totalPages) return

        binding.progressBar.visibility = View.VISIBLE
        
        val bitmap = renderPage(pageIndex)
        if (bitmap != null) {
            binding.imageReport.setImageBitmap(bitmap)
            binding.imageReport.scaleX = 1f
            binding.imageReport.scaleY = 1f
        }
        
        binding.progressBar.visibility = View.GONE
        currentPage = pageIndex
        updateNavigationState()
        
        // Pre-render adjacent pages in background for smoother navigation
        preRenderAdjacentPages(pageIndex)
    }
    
    /**
     * Pre-render adjacent pages for smoother navigation.
     */
    private fun preRenderAdjacentPages(currentIndex: Int) {
        // Render previous and next pages in cache if not already cached
        if (currentIndex > 0 && pageCache.get(currentIndex - 1) == null) {
            renderPage(currentIndex - 1)
        }
        if (currentIndex < totalPages - 1 && pageCache.get(currentIndex + 1) == null) {
            renderPage(currentIndex + 1)
        }
    }

    private fun updateNavigationState() {
        binding.textPageInfo.text = getString(
            R.string.page_info,
            currentPage + 1,
            totalPages
        )
        binding.btnPrevious.isEnabled = currentPage > 0
        binding.btnNext.isEnabled = currentPage < totalPages - 1
    }

    private fun shareReport() {
        val filePath = intent.getStringExtra(EXTRA_PDF_PATH) ?: return
        val file = File(filePath)
        
        if (!file.exists()) {
            Toast.makeText(this, R.string.error_file_not_found, Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val uri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            startActivity(Intent.createChooser(shareIntent, getString(R.string.share_report)))
        } catch (e: Exception) {
            Toast.makeText(
                this,
                getString(R.string.error_sharing, e.message),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clean up resources
        pageCache.evictAll()
        pdfRenderer?.close()
        fileDescriptor?.close()
    }

    companion object {
        private const val EXTRA_PDF_PATH = "pdf_path"
        private const val MAX_CACHED_PAGES = 5 // Limit cached pages to prevent memory issues

        fun newIntent(context: Context, pdfFile: File): Intent {
            return Intent(context, ForensicReportViewerActivity::class.java).apply {
                putExtra(EXTRA_PDF_PATH, pdfFile.absolutePath)
            }
        }
    }
}
