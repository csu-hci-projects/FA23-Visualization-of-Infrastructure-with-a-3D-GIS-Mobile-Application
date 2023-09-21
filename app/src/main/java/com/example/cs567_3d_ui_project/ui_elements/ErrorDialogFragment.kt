package com.example.cs567_3d_ui_project.ui_elements

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.fragment.app.DialogFragment

class ErrorDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            builder.setMessage("Error")
                .setNegativeButton("Cancel", DialogInterface.OnClickListener{ _, _ ->
                    //User Canceled the dialog
                })
            builder.create()

        } ?: throw IllegalStateException("Activity cannot be null")
    }

}
