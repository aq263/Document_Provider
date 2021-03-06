package com.wa2c.android.cifsdocumentsprovider.presentation.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import com.wa2c.android.cifsdocumentsprovider.R
import com.wa2c.android.cifsdocumentsprovider.common.values.Language
import com.wa2c.android.cifsdocumentsprovider.common.values.UiTheme
import com.wa2c.android.cifsdocumentsprovider.databinding.FragmentSettingsBinding
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.MainViewModel
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.*

class SettingsFragment: Fragment(R.layout.fragment_settings) {

    /** View Model */
    private val mainViewModel by activityViewModels<MainViewModel>()
    /** View Model */
    private val viewModel by activityViewModels<SettingsViewModel>()
    /** Binding */
    private val binding: FragmentSettingsBinding? by viewBinding()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setHasOptionsMenu(true)
        (activity as? AppCompatActivity)?.supportActionBar?.let {
            it.setIcon(null)
            it.setTitle(R.string.settings_title)
            it.setDisplayShowHomeEnabled(false)
            it.setDisplayHomeAsUpEnabled(true)
            it.setDisplayShowTitleEnabled(true)
        }

        binding?.let { bind ->
            bind.viewModel = viewModel

            // Settings
            bind.settingsThemeText.setOnClickListener {
                val title = bind.settingsThemeText.text.toString()
                val items = UiTheme.values().map { it.getLabel(requireContext()) }.toTypedArray()
                val selected = viewModel.uiTheme.index
                navigateSafe(SettingsFragmentDirections.actionSettingsFragmentToListDialog(DIALOG_KEY_THEME, title, items, selected))
            }
            bind.settingsLanguageText.setOnClickListener {
                val title = bind.settingsLanguageText.text.toString()
                val items = Language.values().map { it.getLabel(requireContext()) }.toTypedArray()
                val selected = viewModel.language.index
                navigateSafe(SettingsFragmentDirections.actionSettingsFragmentToListDialog(DIALOG_KEY_LANGUAGE, title, items, selected))
            }

            // Information
            bind.settingsContributorText.setOnClickListener {
                openUrl("https://github.com/wa2c/cifs-documents-provider/graphs/contributors")
            }
            bind.settingsLibraryText.setOnClickListener {
                OssLicensesMenuActivity.setActivityTitle(getString(R.string.settings_title))
                startActivity(Intent(requireActivity(), OssLicensesMenuActivity::class.java))
            }
            bind.settingsInfoText.setOnClickListener {
                startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + requireContext().packageName)))
            }
        }

        setFragmentResultListener(DIALOG_KEY_THEME) { _, result ->
            val theme = UiTheme.findByIndexOrDefault(result.getInt(ListDialog.DIALOG_RESULT_INDEX, -1))
            viewModel.uiTheme = theme
            AppCompatDelegate.setDefaultNightMode(theme.mode)
        }
        setFragmentResultListener(DIALOG_KEY_LANGUAGE) { _, result ->
            val language = Language.findByIndexOrDefault(result.getInt(ListDialog.DIALOG_RESULT_INDEX, -1))
            viewModel.language = language
            mainViewModel.updateLanguage(language)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                navigateBack()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun openUrl(url: String) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    companion object {
        private const val DIALOG_KEY_THEME = "DIALOG_KEY_THEME"
        private const val DIALOG_KEY_LANGUAGE = "DIALOG_KEY_LANGUAGE"
    }

}