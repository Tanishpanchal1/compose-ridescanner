package com.example.jetpack_test1

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.*
import java.util.regex.Pattern

/**
 * Dual-function Accessibility Service:
 * 1. Provides smart price estimates (primary feature)
 * 2. Opens apps with destination pre-filled (bonus feature)
 */
class AccessibilityAutomationService : AccessibilityService() {

    companion object {
        var instance: AccessibilityAutomationService? = null
    }

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var currentJob: Job? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.d("ACCESSIBILITY", "🔧 Enhanced automation service ready")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event?.let {
            val packageName = it.packageName?.toString() ?: return
            if (packageName.contains("uber") || packageName.contains("ola") || packageName.contains("rapido")) {
                Log.d("APP_EVENT", "📱 Active in: $packageName")
            }
        }
    }

    override fun onInterrupt() {
        Log.d("ACCESSIBILITY", "Service interrupted")
        currentJob?.cancel()
    }

    // ============================================================================
    // ORIGINAL EXTRACTION METHOD (For fallback compatibility)
    // ============================================================================

    /**
     * Original extraction method - now just returns empty list to trigger smart estimates
     * This maintains compatibility with existing RealTimeAutomationService
     */
    fun startRideExtraction(
        pickup: LocationCoords,
        dropoff: LocationCoords,
        callback: (List<RideOption>) -> Unit
    ) {
        Log.d("AUTOMATION", "🎯 Using smart estimation system (more reliable)")
        // Return empty list immediately to trigger smart estimates in RealTimeAutomationService
        callback(emptyList())
    }

    // ============================================================================
    // NEW APP AUTOMATION FEATURE
    // ============================================================================

    /**
     * NEW FEATURE: Opens apps with destination pre-filled
     * This runs alongside the smart estimation system
     */
    fun openAppsWithDestination(
        destination: String,
        onProgress: (String) -> Unit,
        onComplete: () -> Unit
    ) {
        Log.d("AUTOMATION", "🚀 Starting app automation for: $destination")

        currentJob?.cancel()
        currentJob = serviceScope.launch {
            try {
                onProgress("Opening apps with your destination...")

                // Process each app
                processApp("Uber", "com.ubercab", destination, onProgress)
                delay(4000) // Give user time to see results

                processApp("Ola", "com.olacabs.customer", destination, onProgress)
                delay(4000)

                processApp("Rapido", "com.rapido.passenger", destination, onProgress)
                delay(2000)

                onProgress("✅ All apps opened! Compare live prices manually")
                delay(3000)
                onComplete()

            } catch (e: Exception) {
                Log.e("AUTOMATION", "App automation failed: ${e.message}")
                onProgress("⚠️ App automation completed with some issues")
                onComplete()
            }
        }
    }

    // ============================================================================
    // APP PROCESSING LOGIC
    // ============================================================================

    private suspend fun processApp(
        appName: String,
        packageName: String,
        destination: String,
        onProgress: (String) -> Unit
    ) {
        Log.d("AUTOMATION", "🔄 Processing $appName...")

        try {
            // Check if app is installed
            if (!isAppInstalled(packageName)) {
                onProgress("⚠️ $appName not installed - skipping")
                return
            }

            onProgress("📱 Opening $appName...")

            // Launch app
            if (!launchApp(packageName)) {
                onProgress("❌ Failed to open $appName")
                return
            }

            delay(5000) // Wait for app to load

            // Handle initial screens
            onProgress("🔄 Setting up $appName...")
            handleInitialScreens()

            // Enter destination
            onProgress("✍️ Entering destination in $appName...")
            val success = enterDestination(destination)

            if (success) {
                triggerSearch()
                onProgress("✅ $appName ready - check prices!")
                Log.d("AUTOMATION", "✅ $appName: Successfully opened with destination")
            } else {
                onProgress("⚠️ $appName opened (manual entry needed)")
                Log.w("AUTOMATION", "⚠️ $appName: Could not auto-fill destination")
            }

        } catch (e: Exception) {
            Log.e("AUTOMATION", "$appName processing failed: ${e.message}")
            onProgress("⚠️ $appName: Error occurred")
        }
    }

    private fun isAppInstalled(packageName: String): Boolean {
        return try {
            packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun launchApp(packageName: String): Boolean {
        return try {
            val intent = packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                startActivity(intent)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e("APP_LAUNCH", "Failed to launch $packageName: ${e.message}")
            false
        }
    }

    private suspend fun handleInitialScreens() {
        repeat(3) {
            delay(1500)
            val rootNode = rootInActiveWindow ?: return

            // Look for common buttons to skip initial screens
            val skipTexts = listOf(
                "Skip", "Maybe Later", "Not Now", "Continue", "Allow",
                "Continue as Guest", "Book Now", "Get Started", "Grant", "OK"
            )

            for (text in skipTexts) {
                val buttons = rootNode.findAccessibilityNodeInfosByText(text)
                if (buttons.isNotEmpty()) {
                    Log.d("UI_AUTO", "Clicking '$text' to proceed")
                    buttons.first().performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    delay(2000)
                    return@repeat
                }
            }
        }
    }

    private suspend fun enterDestination(destination: String): Boolean {
        Log.d("UI_AUTO", "🎯 Starting destination entry for: $destination")

        // Give app more time to fully load
        delay(2000)

        repeat(8) { attempt ->
            Log.d("UI_AUTO", "📝 Destination entry attempt ${attempt + 1}/8")

            val rootNode = rootInActiveWindow
            if (rootNode == null) {
                Log.w("UI_AUTO", "No root node available, waiting...")
                delay(2000)
                return@repeat
            }

            // Debug: Log the current screen structure
            logScreenStructure(rootNode)

            // Try multiple strategies in order of reliability
            val strategies = listOf(
                { findDestinationByHint(rootNode) },
                { findDestinationByText(rootNode) },
                { findDestinationByClassName(rootNode) },
                { findAnyEditText(rootNode) }
            )

            for ((index, strategy) in strategies.withIndex()) {
                Log.d("UI_AUTO", "Trying strategy ${index + 1}...")

                val field = strategy()
                if (field != null && tryEnterText(field, destination)) {
                    Log.d("UI_AUTO", "✅ SUCCESS: Destination entered using strategy ${index + 1}")
                    return true
                }
            }

            // Wait before next attempt
            delay(2500)
        }

        Log.w("UI_AUTO", "❌ FAILED: Could not enter destination after all attempts")
        return false
    }

    /**
     * Robust text entry with multiple fallback methods
     */
    private suspend fun tryEnterText(field: AccessibilityNodeInfo, text: String): Boolean {
        Log.d("UI_AUTO", "💬 Attempting to enter text in field: ${field.className}")
        Log.d("UI_AUTO", "Field details - Text: '${field.text}', Hint: '${field.hintText}', Enabled: ${field.isEnabled}, Editable: ${field.isEditable}")

        // Method 1: Direct text setting (most reliable)
        if (setTextDirectly(field, text)) {
            Log.d("UI_AUTO", "✅ Direct text setting worked")
            return true
        }

        delay(1000)

        // Method 2: Click first, then set text
        if (clickAndSetText(field, text)) {
            Log.d("UI_AUTO", "✅ Click and set text worked")
            return true
        }

        delay(1000)

        // Method 3: Focus, clear, then set text
        if (focusClearAndSetText(field, text)) {
            Log.d("UI_AUTO", "✅ Focus, clear and set text worked")
            return true
        }

        delay(1000)

        // Method 4: Simulate typing (last resort)
        if (simulateTyping(field, text)) {
            Log.d("UI_AUTO", "✅ Simulated typing worked")
            return true
        }

        Log.w("UI_AUTO", "❌ All text entry methods failed for this field")
        return false
    }

    private suspend fun setTextDirectly(field: AccessibilityNodeInfo, text: String): Boolean {
        return try {
            val bundle = android.os.Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
            }

            val result = field.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, bundle)
            delay(1500) // Wait for text to be processed
            result
        } catch (e: Exception) {
            Log.w("UI_AUTO", "Direct text setting failed: ${e.message}")
            false
        }
    }

    private suspend fun clickAndSetText(field: AccessibilityNodeInfo, text: String): Boolean {
        return try {
            // Click to focus
            field.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            delay(1000)

            // Set text
            val bundle = android.os.Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
            }

            val result = field.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, bundle)
            delay(1500)
            result
        } catch (e: Exception) {
            Log.w("UI_AUTO", "Click and set text failed: ${e.message}")
            false
        }
    }

    private suspend fun focusClearAndSetText(field: AccessibilityNodeInfo, text: String): Boolean {
        return try {
            // Focus the field
            field.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
            delay(500)

            // Clear existing content by setting empty text first
            val clearBundle = android.os.Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, "")
            }
            field.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, clearBundle)
            delay(500)

            // Set the actual text
            val textBundle = android.os.Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
            }

            val result = field.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, textBundle)
            delay(1500)
            result
        } catch (e: Exception) {
            Log.w("UI_AUTO", "Focus, clear and set text failed: ${e.message}")
            false
        }
    }

    private suspend fun simulateTyping(field: AccessibilityNodeInfo, text: String): Boolean {
        return try {
            // Click to focus
            field.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            delay(1000)

            // Type character by character (slower but sometimes more reliable)
            text.forEach { char ->
                val bundle = android.os.Bundle().apply {
                    putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, char.toString())
                }
                field.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, bundle)
                delay(200) // Delay between characters
            }

            delay(1000)
            true
        } catch (e: Exception) {
            Log.w("UI_AUTO", "Simulated typing failed: ${e.message}")
            false
        }
    }

    /**
     * Enhanced field detection strategies
     */
    private fun findDestinationByHint(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val hints = listOf(
            "where to", "destination", "drop off", "going to", "to",
            "enter destination", "search destination", "drop location",
            "where do you want to go", "choose destination"
        )

        return findFieldByProperty(root) { node ->
            val hint = node.hintText?.toString()?.lowercase() ?: ""
            hints.any { hint.contains(it) } && node.isEditable
        }
    }

    private fun findDestinationByText(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val textMatches = listOf(
            "where to", "destination", "search", "to", "drop off"
        )

        return findFieldByProperty(root) { node ->
            val text = node.text?.toString()?.lowercase() ?: ""
            textMatches.any { text.contains(it) } && node.isEditable
        }
    }

    private fun findDestinationByClassName(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        return findFieldByProperty(root) { node ->
            val className = node.className?.toString() ?: ""
            (className.contains("EditText") || className.contains("TextInputEditText")) &&
                    node.isEditable && node.isEnabled
        }
    }

    private fun findAnyEditText(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        return findFieldByProperty(root) { node ->
            node.isEditable && node.isEnabled && node.isFocusable
        }
    }

    private fun findFieldByProperty(root: AccessibilityNodeInfo, predicate: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo? {
        fun searchRecursively(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
            if (predicate(node)) {
                Log.d("UI_AUTO", "Found matching field: ${node.className}, text: '${node.text}', hint: '${node.hintText}'")
                return node
            }

            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                val result = searchRecursively(child)
                if (result != null) return result
            }

            return null
        }

        return searchRecursively(root)
    }

    /**
     * Debug helper - logs screen structure to understand app layout
     */
    private fun logScreenStructure(root: AccessibilityNodeInfo, depth: Int = 0) {
        if (depth > 3) return // Limit depth to avoid spam

        val indent = "  ".repeat(depth)
        val className = root.className?.toString()?.substringAfterLast('.') ?: "Unknown"
        val text = root.text?.toString() ?: ""
        val hint = root.hintText?.toString() ?: ""
        val contentDesc = root.contentDescription?.toString() ?: ""

        if (text.isNotEmpty() || hint.isNotEmpty() || contentDesc.isNotEmpty() || root.isEditable) {
            Log.d("SCREEN_STRUCTURE", "$indent$className - Text:'$text', Hint:'$hint', Desc:'$contentDesc', Editable:${root.isEditable}")
        }

        for (i in 0 until root.childCount) {
            val child = root.getChild(i) ?: continue
            logScreenStructure(child, depth + 1)
        }
    }

    /**
     * Enhanced search triggering with multiple strategies
     */
//    private suspend fun triggerSearch() {
//        delay(1000)
//
//        val rootNode = rootInActiveWindow ?: return
//
//        // Strategy 1: Look for explicit search buttons
//        val searchTexts = listOf(
//            "search", "find rides", "book", "continue", "go", "submit", "next", "done", "confirm"
//        )
//
//        for (text in searchTexts) {
//            val buttons = rootNode.findAccessibilityNodeInfosByText(text)
//            for (button in buttons) {
//                if (button.isClickable) {
//                    Log.d("UI_AUTO", "🔍 Triggering search with button: $text")
//                    button.performAction(AccessibilityNodeInfo.ACTION_CLICK)
//                    delay(2000)
//                    return
//                }
//            }
//        }
//
//        // Strategy 2: Look for any clickable element with "search" in description
//        val searchButton = findFieldByProperty(rootNode) { node ->
//            val desc = node.contentDescription?.toString()?.lowercase() ?: ""
//            node.isClickable && desc.contains("search")
//        }
//
//        if (searchButton != null) {
//            Log.d("UI_AUTO", "🔍 Found search button by description")
//            searchButton.performAction(AccessibilityNodeInfo.ACTION_CLICK)
//            delay(2000)
//            return
//        }
//
//        // Strategy 3: Try global back action (sometimes triggers search)
//        try {
//            Log.d("UI_AUTO", "🔍 Trying global back action to trigger search")
//            performGlobalAction(GLOBAL_ACTION_BACK)
//            delay(1000)
//        } catch (e: Exception) {
//            Log.d("UI_AUTO", "🔍 Search trigger completed with back action")
//        }
//    }



    private fun findDestinationField(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        // Strategy 1: Look for common destination-related text
        val destinationKeywords = listOf(
            "Where to", "Destination", "Drop off", "Going to",
            "Enter destination", "Search destination", "To", "Drop location"
        )

        for (keyword in destinationKeywords) {
            val nodes = root.findAccessibilityNodeInfosByText(keyword)
            for (node in nodes) {
                // Check if it's directly editable
                if (node.isEditable) return node

                // Check if it's a label for an editable field
                val editableNearby = findEditableNearby(node)
                if (editableNearby != null) return editableNearby
            }
        }

        // Strategy 2: Find any editable text field (might be destination field)
        return findFirstEmptyEditText(root)
    }

    private fun findEditableNearby(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        // Check siblings
        val parent = node.parent
        if (parent != null) {
            for (i in 0 until parent.childCount) {
                val sibling = parent.getChild(i) ?: continue
                if (sibling.isEditable) return sibling

                // Check sibling's children
                for (j in 0 until sibling.childCount) {
                    val child = sibling.getChild(j) ?: continue
                    if (child.isEditable) return child
                }
            }
        }

        return null
    }

    private fun findFirstEmptyEditText(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        // Look for EditText that's empty or has placeholder text
        fun searchRecursive(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
            if (node.className?.contains("EditText") == true && node.isEditable) {
                val text = node.text?.toString() ?: ""
                // Prefer empty fields or fields with common placeholder text
                if (text.isEmpty() ||
                    text.contains("Where to", ignoreCase = true) ||
                    text.contains("Destination", ignoreCase = true) ||
                    text.contains("Search", ignoreCase = true)) {
                    return node
                }
            }

            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                val result = searchRecursive(child)
                if (result != null) return result
            }

            return null
        }

        return searchRecursive(root)
    }

    private suspend fun triggerSearch() {
        delay(1000)
        val rootNode = rootInActiveWindow ?: return

        // Look for search/submit buttons
        val searchTexts = listOf(
            "Search", "Find rides", "Book", "Continue", "Go", "Submit", "Next", "Done"
        )

        for (text in searchTexts) {
            val buttons = rootNode.findAccessibilityNodeInfosByText(text)
            if (buttons.isNotEmpty()) {
                Log.d("UI_AUTO", "Triggering search with '$text'")
                buttons.first().performAction(AccessibilityNodeInfo.ACTION_CLICK)
                delay(1000)
                return
            }
        }

        // Fallback: try pressing back or enter
        try {
            performGlobalAction(GLOBAL_ACTION_BACK)
        } catch (e: Exception) {
            Log.d("UI_AUTO", "Search trigger completed")
        }
    }
}
