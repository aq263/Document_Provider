package com.wa2c.android.cifsdocumentsprovider.presentation.ui.send

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.wa2c.android.cifsdocumentsprovider.R
import com.wa2c.android.cifsdocumentsprovider.common.utils.logD
import com.wa2c.android.cifsdocumentsprovider.databinding.FragmentSendBinding
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.collectIn
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.viewBinding
import dagger.hilt.android.AndroidEntryPoint

/**
 * Send Screen
 */
@AndroidEntryPoint
class SendFragment: Fragment(R.layout.fragment_send) {

    /** View Model */
    private val viewModel by activityViewModels<SendViewModel>()
    /** Binding */
    private val binding: FragmentSendBinding? by viewBinding()
    /** List adapter */
    private val adapter: SendListAdapter by lazy { SendListAdapter(viewModel) }
    /** Arguments */
    private val args: SendFragmentArgs by navArgs()

    /** Single URI result launcher */
    private val singleUriLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument()) { uri ->
        if (uri == null) {
            activity?.finishAffinity()
            return@registerForActivityResult
        }

        val source = args.inputUris.firstOrNull() ?: let {
            activity?.finishAffinity()
            return@registerForActivityResult
        }

        viewModel.sendUri(listOf(source), uri)
    }

    /** Multiple URI result launcher */
    private val multipleUriLauncher = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri == null) {
            activity?.finishAffinity()
            return@registerForActivityResult
        }

        val source = args.inputUris.toList().ifEmpty {
            activity?.finishAffinity()
            return@registerForActivityResult
        }

        viewModel.sendUri(source, uri)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Disable back key
        activity?.onBackPressedDispatcher?.addCallback(viewLifecycleOwner) {
            confirmClose()
        }

        setHasOptionsMenu(true)
        (activity as? AppCompatActivity)?.supportActionBar?.let {
            it.setIcon(null)
            it.setTitle(R.string.send_title)
            it.setDisplayShowHomeEnabled(false)
            it.setDisplayHomeAsUpEnabled(false)
            it.setDisplayShowTitleEnabled(true)
        }

        binding?.let { bind ->
            bind.viewModel = viewModel
            bind.sendList.adapter = adapter
            bind.sendList.itemAnimator = null
            bind.sendList.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
        }

        viewModel.let { vm ->
            vm.navigationEvent.collectIn(viewLifecycleOwner, observer = ::onNavigate)
            vm.sendDataList.collectIn(viewLifecycleOwner) { list ->
                adapter.submitList(list)
                updateCancelButton()
            }
            vm.sendData.collectIn(viewLifecycleOwner) { data ->
                val index = vm.sendDataList.value.indexOfLast { it == data }
                if (index < 0) return@collectIn
                adapter.notifyItemChanged(index)
                updateCancelButton()
            }
            vm.updateIndex.collectIn(viewLifecycleOwner) { index ->
                if (index < 0) return@collectIn
                adapter.notifyItemChanged(index)
                updateCancelButton()
            }
        }

        val uris = args.inputUris
        when {
            uris.size == 1 -> {
                // Single
                val uri = uris.first()
                val file = DocumentFile.fromSingleUri(requireContext(), uri)
                singleUriLauncher.launch(file?.name ?: uri.lastPathSegment)
            }
            uris.size > 1 -> {
                // Multiple
                multipleUriLauncher.launch(args.inputUris.first())
            }
        }
    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_send, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.send_menu_close -> {
                confirmClose()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    /**
     * Confirm closing.
     */
    private fun confirmClose() {
        MaterialAlertDialogBuilder(requireContext())
            .setMessage(R.string.send_exit_confirmation_message)
            .setPositiveButton(R.string.dialog_accept) { _, _ ->
                activity?.finishAffinity()
            }
            .setNeutralButton(R.string.dialog_close, null)
            .show()
    }

    private fun updateCancelButton() {
        binding?.sendCancelButton?.isEnabled = viewModel.sendDataList.value.any { it.state.isCancelable }
    }

    private fun onNavigate(event: SendNav) {
        logD("onNavigate: event=$event")
        when (event) {
            is SendNav.ConfirmOverwrite -> {
                MaterialAlertDialogBuilder(requireContext())
                    .setMessage(getString(R.string.send_overwrite_confirmation_message, event.overwriteIdSet.size))
                    .setPositiveButton(R.string.dialog_accept) { _, _ ->
                        viewModel.updateToReady(event.overwriteIdSet)
                    }
                    .setNeutralButton(R.string.dialog_close, null)
                    .show()
            }
        }
    }

}
