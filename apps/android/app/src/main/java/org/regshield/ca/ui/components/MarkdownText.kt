package org.regshield.ca.ui.components

import android.content.Intent
import android.net.Uri
import android.text.method.LinkMovementMethod
import android.util.TypedValue
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.LinkResolver
import io.noties.markwon.Markwon
import io.noties.markwon.MarkwonConfiguration
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.ext.tasklist.TaskListPlugin
import java.util.Locale
import org.regshield.ca.R

private val ALLOWED_LINK_DOMAINS =
  setOf(
    "uscis.gov",
    "justice.gov",
    "eoir.justice.gov",
    "ca.gov",
    "calbar.ca.gov",
    "sos.ca.gov",
    "lawhelpca.org",
    "immigrationadvocates.org",
  )

private fun isAllowedUrl(raw: String): Boolean {
  return try {
    val uri = Uri.parse(raw)
    val scheme = uri.scheme?.lowercase(Locale.US)
    if (scheme != "http" && scheme != "https") return false
    val host = uri.host?.lowercase(Locale.US) ?: return false
    ALLOWED_LINK_DOMAINS.any { d -> host == d || host.endsWith(".$d") }
  } catch (_: Exception) {
    false
  }
}

private fun openExternal(view: View, url: String) {
  val ctx = view.context
  val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
  intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
  runCatching { ContextCompat.startActivity(ctx, intent, null) }
}

@Composable
fun MarkdownText(
  markdown: String,
  modifier: Modifier = Modifier,
  style: TextStyle,
  textColor: Color,
  linkColor: Color,
  onOpenUrl: (String) -> Unit,
  // For androidTest: allows locating the underlying TextView.
  viewTag: String? = null,
) {
  val context = LocalContext.current
  val density = LocalDensity.current
  val openUrlState = rememberUpdatedState(onOpenUrl)
  val bodyTypeface =
    remember(context) { runCatching { ResourcesCompat.getFont(context, R.font.manrope_regular) }.getOrNull() }

  val markwon =
    remember(context) {
      val linkResolver =
        LinkResolver { view, link ->
          if (isAllowedUrl(link)) {
            // Prefer the caller handler (usually Intent open). If it throws, fall back to best-effort.
            runCatching { openUrlState.value(link) }.onFailure { openExternal(view, link) }
          } else {
            Toast
              .makeText(view.context, "Blocked link (untrusted source).", Toast.LENGTH_SHORT)
              .show()
          }
        }

      Markwon.builder(context)
        .usePlugin(StrikethroughPlugin.create())
        .usePlugin(TaskListPlugin.create(context))
        .usePlugin(TablePlugin.create(context))
        .usePlugin(
          object : AbstractMarkwonPlugin() {
            override fun configureConfiguration(builder: MarkwonConfiguration.Builder) {
              builder.linkResolver(linkResolver)
            }
          },
        )
        .build()
    }

  val fontSizeSp = if (style.fontSize.isSpecified) style.fontSize else 16.sp
  val lineHeightSp = style.lineHeight

  AndroidView(
    modifier = modifier,
    factory = { ctx ->
      TextView(ctx).apply {
        // Prevent list markers / leading spans from being clipped at the view edge.
        val pad = (2f * ctx.resources.displayMetrics.density).toInt().coerceAtLeast(0)
        setPaddingRelative(pad, 0, pad, 0)
        includeFontPadding = false
        movementMethod = LinkMovementMethod.getInstance()
        highlightColor = android.graphics.Color.TRANSPARENT
        textAlignment = TextView.TEXT_ALIGNMENT_VIEW_START
        setTextIsSelectable(false)
        // Ensure Markdown links are clickable.
        linksClickable = true
        // For tests (optional).
        if (!viewTag.isNullOrBlank()) tag = viewTag
        // Font.
        bodyTypeface?.let { typeface = it }
      }
    },
    update = { tv ->
      tv.setTextColor(textColor.toArgb())
      tv.setLinkTextColor(linkColor.toArgb())
      tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSizeSp.value)

      if (lineHeightSp.isSpecified) {
        val targetPx = with(density) { lineHeightSp.toPx() }
        val fm = tv.paint.fontMetrics
        val fontPx = (fm.descent - fm.ascent).coerceAtLeast(1f)
        val extra = (targetPx - fontPx).coerceAtLeast(0f)
        tv.setLineSpacing(extra, 1f)
      } else {
        tv.setLineSpacing(0f, 1f)
      }

      // Rendering is idempotent; markwon will handle spans and link taps.
      markwon.setMarkdown(tv, markdown)
    },
  )
}
