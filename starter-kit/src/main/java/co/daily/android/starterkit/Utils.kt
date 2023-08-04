package co.daily.android.starterkit

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.annotation.MenuRes
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.PopupMenu
import co.daily.core.dailydemo.R


object Utils {

    fun listenForTextChange(view: EditText, action: (String) -> Unit) {
        view.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable) {
                action(s.toString())
            }
        })
    }

    fun copyMeetingLinkToClipboard(context: Context, link: String) {
        val service = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        service.setPrimaryClip(ClipData.newPlainText(link, link))

        Toast.makeText(context, R.string.link_copied_to_clipboard, Toast.LENGTH_SHORT).show()
    }

    fun showPopupMenu(context: Context, anchor: View, @MenuRes menuRes: Int, onClick: (Int) -> Unit) {
        PopupMenu(ContextThemeWrapper(context, R.style.Widget_Daily_PopupMenu), anchor).apply {

            menuInflater.inflate(menuRes, menu)
            setForceShowIcon(true)

            setOnMenuItemClickListener {
                onClick(it.itemId)
                true
            }

            show()
        }
    }
}
