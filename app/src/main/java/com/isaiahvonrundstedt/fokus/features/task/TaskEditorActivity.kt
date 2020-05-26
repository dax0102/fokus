package com.isaiahvonrundstedt.fokus.features.task

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.Menu
import android.view.View
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.afollestad.materialdialogs.LayoutMode
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.bottomsheets.BottomSheet
import com.afollestad.materialdialogs.datetime.dateTimePicker
import com.afollestad.materialdialogs.lifecycle.lifecycleOwner
import com.afollestad.materialdialogs.list.customListAdapter
import com.google.android.material.chip.Chip
import com.isaiahvonrundstedt.fokus.R
import com.isaiahvonrundstedt.fokus.features.attachments.Attachment
import com.isaiahvonrundstedt.fokus.features.core.data.Core
import com.isaiahvonrundstedt.fokus.features.shared.PermissionManager
import com.isaiahvonrundstedt.fokus.features.shared.abstracts.BaseActivity
import com.isaiahvonrundstedt.fokus.features.shared.components.adapters.SubjectListAdapter
import com.isaiahvonrundstedt.fokus.features.subject.Subject
import com.isaiahvonrundstedt.fokus.features.subject.SubjectActivity
import com.isaiahvonrundstedt.fokus.features.subject.SubjectViewModel
import kotlinx.android.synthetic.main.layout_appbar_editor.*
import kotlinx.android.synthetic.main.layout_editor_task.*
import org.joda.time.DateTime
import org.joda.time.LocalDateTime
import java.util.*
import kotlin.collections.ArrayList

class TaskEditorActivity: BaseActivity(), SubjectListAdapter.ItemSelected {

    private var requestCode = 0
    private var core: Core? = null
    private var task = Task()

    private val attachmentRequestCode = 32
    private val attachmentList = ArrayList<Attachment>()
    private val subjectViewModel: SubjectViewModel by lazy {
        ViewModelProvider(this).get(SubjectViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_editor_task)
        setPersistentActionBar(toolbar)

        // Check if the parent activity has extras sent then
        // determine if the editorUI will be in insert or
        // update mode
        requestCode = if (intent.hasExtra(extraTask) && intent.hasExtra(extraSubject)
            && intent.hasExtra(extraAttachments)) updateRequestCode else insertRequestCode
        if (requestCode == updateRequestCode) {
            task = intent.getParcelableExtra(extraTask)!!
            subject = intent.getParcelableExtra(extraSubject)!!
            attachmentList.clear()
            attachmentList.addAll(intent.getParcelableArrayListExtra(extraAttachments) ?: emptyList())
        }

        subjectViewModel.fetch()?.observe(this, Observer { items ->
            adapter.setObservableItems(items)
        })

        // the passed extras from the parent activity
        // will be shown in their respective fields.
        if (requestCode == updateRequestCode) {
            nameEditText.setText(task.name)
            notesEditText.setText(task.notes)
            subjectTextView.text = subject!!.code
            dueDateTextView.text = task.formatDueDate(this)

            subjectTextView.editorValueChanged()
            dueDateTextView.editorValueChanged()

            attachmentList.forEach { attachment ->
                attachmentChipGroup.addView(buildChip(attachment), 0)
            }
        }
    }

    private var adapter = SubjectListAdapter(this)
    private var subjectDialog: MaterialDialog? = null
    override fun onStart() {
        super.onStart()

        dueDateTextView.setOnClickListener { v ->
            MaterialDialog(this).show {
                lifecycleOwner(this@TaskEditorActivity)
                dateTimePicker(requireFutureDateTime = true,
                    currentDateTime = task.dueDate?.toDateTime()?.toCalendar(Locale.getDefault())) { _, datetime ->
                    task.dueDate = LocalDateTime.fromCalendarFields(datetime).toDateTime()
                }
                positiveButton(R.string.button_done) {
                    if (v is AppCompatTextView) {
                        v.text = task.formatDueDate(this@TaskEditorActivity)
                        v.editorValueChanged()
                    }
                }
            }
        }

        subjectTextView.setOnClickListener {
            subjectDialog = MaterialDialog(this, BottomSheet(LayoutMode.WRAP_CONTENT)).show {
                lifecycleOwner(this@TaskEditorActivity)
                title(R.string.dialog_select_subject_title)
                message(R.string.dialog_select_subject_summary)
                customListAdapter(adapter)
                positiveButton(R.string.button_new_subject) {
                    startActivity(Intent(this@TaskEditorActivity, SubjectActivity::class.java))
                }
            }
        }

        attachmentChip.setOnClickListener {
            // Check if we have read storage permissions then request the permission
            // if we have the permission, open up file picker
            if (PermissionManager(this).readAccessGranted) {
                startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT)
                    .setType("*/*"), attachmentRequestCode)
            } else
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    PermissionManager.readStorageRequestCode)
        }

        actionButton.setOnClickListener {

            // This three ifs checks if the user have entered the
            // values on the fields, if we don't have the value required,
            // show a snackbar feedback then direct the user's
            // attention to the field. Then return to stop the execution
            // of the code.
            if (nameEditText.text.isNullOrEmpty()) {
                createSnackbar(rootLayout, R.string.feedback_task_empty_name)
                nameEditText.requestFocus()
                return@setOnClickListener
            }

            if (task.dueDate == null) {
                createSnackbar(rootLayout, R.string.feedback_task_empty_due_date)
                dueDateTextView.performClick()
                return@setOnClickListener
            }

            if (task.subjectID == null) {
                createSnackbar(rootLayout, R.string.feedback_task_empty_subject)
                subjectTextView.performClick()
                return@setOnClickListener
            }

            task.name = nameEditText.text.toString()
            task.notes = notesEditText.text.toString()

            // Prepare the send the data back to the parent activity
            core = Core(task = this.task, subject = this.subject!!,
                attachmentList = this.attachmentList)

            // Send the data back to the parent activity
            val data = Intent()
            data.putExtra(extraTask, core?.task)
            data.putParcelableArrayListExtra(extraAttachments, ArrayList(core?.attachmentList!!))
            setResult(Activity.RESULT_OK, data)
            finish()
        }
    }

    // Item selection callback for the subject dialog
    private var subject: Subject? = null
    override fun onItemSelected(subject: Subject) {
        task.subjectID = subject.id
        this.subject = subject

        subjectTextView.text = subject.code
        subjectTextView.editorValueChanged()
        subjectDialog?.dismiss()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>,
                                            grantResults: IntArray) {
        if (requestCode == PermissionManager.readStorageRequestCode
            && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT)
                .setType("*/*"), attachmentRequestCode)
        } else
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        // Check if the request codes are the same and the result code is successful
        // then create an Attachment object and a corresponding ChipView
        // attachments have to be inserted temporarily on the ArrayList
        if (requestCode == attachmentRequestCode && resultCode == Activity.RESULT_OK) {
            contentResolver?.takePersistableUriPermission(data?.data!!,
                Intent.FLAG_GRANT_READ_URI_PERMISSION)

            val attachment = Attachment().apply {
                taskID = task.taskID
                uri = data?.data
                dateAttached = DateTime.now()
            }

            attachmentList.add(attachment)
            attachmentChipGroup.addView(buildChip(attachment), 0)
        } else
            super.onActivityResult(requestCode, resultCode, data)
    }

    private fun buildChip(attachment: Attachment): Chip {
        return Chip(this).apply {
            text = getFileName(attachment.uri!!)
            tag = attachment.id
            isCloseIconVisible = true
            setOnClickListener(chipClickListener)
            setOnCloseIconClickListener(chipRemoveListener)
        }
    }

    // An extension function for the attachment list that will get the item
    // using the items id from the list
    private fun ArrayList<Attachment>.getUsingID(id: String): Attachment? {
        this.forEach { if (it.id == id) return it }
        return null
    }

    private val chipClickListener = View.OnClickListener {
        val attachment = attachmentList.getUsingID(it.tag.toString())
        if (attachment != null) onParseIntent(attachment.uri)
    }

    private val chipRemoveListener = View.OnClickListener {
        val index = attachmentChipGroup.indexOfChild(it)
        attachmentChipGroup.removeViewAt(index)

        val attachment = attachmentList.getUsingID(it.tag.toString())
        if (attachment != null) attachmentList.remove(attachment)
    }

    // This function determines the file name for the uri returned by the SAF file
    // picker that will be used exclusively for the ChipView
    private fun getFileName(uri: Uri): String {
        var result = ""
        if (uri.scheme == "content") {
            val cursor: Cursor? = contentResolver?.query(uri, null, null, null, null)
            try {
                if (cursor != null && cursor.moveToFirst())
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))
            } catch (ex: Exception) {}
            finally { cursor?.close() }
        } else {
            result = uri.path.toString()
            val index = result.lastIndexOf('/')
            if (index != 1)
                result = result.substring(index + 1)
        }
        return result
    }

    // This function invokes the corresponding application that
    // will open the uri of the attachment if the user clicks
    // on the ChipView
    private fun onParseIntent(uri: Uri?) {
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, contentResolver?.getType(uri!!))
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        if (intent.resolveActivity(packageManager!!) != null)
            startActivity(intent)
    }

    // Override this function to remove the default menu
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        return true
    }

    companion object {
        const val insertRequestCode = 32
        const val updateRequestCode = 19

        const val extraTask = "extraTask"
        const val extraSubject = "extraSubject"
        const val extraAttachments = "extraAttachments"
    }

}