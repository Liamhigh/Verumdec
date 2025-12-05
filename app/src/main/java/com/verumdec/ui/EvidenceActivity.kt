package com.verumdec.ui

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.verumdec.R
import com.verumdec.forensic.CaseResult

class EvidenceActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_evidence)
        
        // Get the case result from intent
        val caseResult = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra("caseResult", CaseResult::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra("caseResult") as? CaseResult
        }
        
        // Display the results
        findViewById<TextView>(R.id.textContradictions)?.text = 
            "Contradictions: ${caseResult?.contradictions ?: "Loading..."}"
        findViewById<TextView>(R.id.textTimeline)?.text = 
            "Timeline: ${caseResult?.timeline ?: "Loading..."}"
        findViewById<TextView>(R.id.textImage)?.text = 
            "Image: ${caseResult?.image ?: "Loading..."}"
        findViewById<TextView>(R.id.textVoice)?.text = 
            "Voice: ${caseResult?.voice ?: "Loading..."}"
        findViewById<TextView>(R.id.textSeal)?.text = 
            "Seal: ${caseResult?.seal ?: "Loading..."}"
    }
}
