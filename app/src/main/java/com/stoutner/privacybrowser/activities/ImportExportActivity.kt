/*
 * Copyright 2018-2023 Soren Stoutner <soren@stoutner.com>.
 *
 * This file is part of Privacy Browser Android <https://www.stoutner.com/privacy-browser-android>.
 *
 * Privacy Browser Android is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Privacy Browser Android is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Privacy Browser Android.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.stoutner.privacybrowser.activities

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.Spinner
import android.widget.TextView

import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.cardview.widget.CardView
import androidx.core.content.FileProvider
import androidx.preference.PreferenceManager

import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputLayout

import com.stoutner.privacybrowser.R
import com.stoutner.privacybrowser.BuildConfig
import com.stoutner.privacybrowser.helpers.EXPORT_SUCCESSFUL
import com.stoutner.privacybrowser.helpers.IMPORT_EXPORT_SCHEMA_VERSION
import com.stoutner.privacybrowser.helpers.IMPORT_SUCCESSFUL
import com.stoutner.privacybrowser.helpers.ImportExportDatabaseHelper

import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.lang.Exception
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Arrays

import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.system.exitProcess

// Define the encryption constants.
private const val NO_ENCRYPTION = 0
private const val PASSWORD_ENCRYPTION = 1
private const val OPENPGP_ENCRYPTION = 2

// Define the saved instance state constants.
private const val ENCRYPTION_PASSWORD_TEXTINPUTLAYOUT_VISIBILITY = "A"
private const val OPEN_KEYCHAIN_REQUIRED_TEXTVIEW_VISIBILITY = "B"
private const val FILE_LOCATION_CARD_VIEW = "C"
private const val FILE_NAME_LINEARLAYOUT_VISIBILITY = "D"
private const val OPEN_KEYCHAIN_IMPORT_INSTRUCTIONS_TEXTVIEW_VISIBILITY = "E"
private const val IMPORT_EXPORT_BUTTON_VISIBILITY = "F"
private const val FILE_NAME_TEXT = "G"
private const val IMPORT_EXPORT_BUTTON_TEXT = "H"

class ImportExportActivity : AppCompatActivity() {
    // Define the class views.
    private lateinit var encryptionSpinner: Spinner
    private lateinit var encryptionPasswordTextInputLayout: TextInputLayout
    private lateinit var encryptionPasswordEditText: EditText
    private lateinit var openKeychainRequiredTextView: TextView
    private lateinit var fileLocationCardView: CardView
    private lateinit var importRadioButton: RadioButton
    private lateinit var fileNameLinearLayout: LinearLayout
    private lateinit var fileNameEditText: EditText
    private lateinit var openKeychainImportInstructionsTextView: TextView
    private lateinit var importExportButton: Button

    // Define the class variables.
    private lateinit var fileProviderDirectory: File
    private var openKeychainInstalled = false
    private lateinit var temporaryPgpEncryptedImportFile: File
    private lateinit var temporaryPreEncryptedExportFile: File

    // Define the browse for import activity result launcher.  It must be defined before `onCreate()` is run or the app will crash.
    private val browseForImportActivityResultLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { fileUri: Uri? ->
        // Only do something if the user didn't press back from the file picker.
        if (fileUri != null) {
            // Get the file name string from the URI.
            val fileNameString = fileUri.toString()

            // Set the file name name text.
            fileNameEditText.setText(fileNameString)

            // Move the cursor to the end of the file name edit text.
            fileNameEditText.setSelection(fileNameString.length)
        }
    }

    private val browseForExportActivityResultLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("*/*")) { fileUri: Uri? ->
        // Only do something if the user didn't press back from the file picker.
        if (fileUri != null) {
            // Get the file name string from the URI.
            val fileNameString = fileUri.toString()

            // Set the file name name text.
            fileNameEditText.setText(fileNameString)

            // Move the cursor to the end of the file name edit text.
            fileNameEditText.setSelection(fileNameString.length)
        }
    }

    private val openKeychainDecryptActivityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        // Delete the temporary PGP encrypted import file.
        if (temporaryPgpEncryptedImportFile.exists())
            temporaryPgpEncryptedImportFile.delete()

        // Delete the file provider directory if it exists.
        if (fileProviderDirectory.exists())
            fileProviderDirectory.delete()
    }

    private val openKeychainEncryptActivityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        // Delete the temporary pre-encrypted export file if it exists.
        if (temporaryPreEncryptedExportFile.exists())
            temporaryPreEncryptedExportFile.delete()

        // Delete the file provider directory if it exists.
        if (fileProviderDirectory.exists())
            fileProviderDirectory.delete()
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        // Get a handle for the shared preferences.
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)

        // Get the preferences.
        val allowScreenshots = sharedPreferences.getBoolean(getString(R.string.allow_screenshots_key), false)
        val bottomAppBar = sharedPreferences.getBoolean(getString(R.string.bottom_app_bar_key), false)

        // Disable screenshots if not allowed.
        if (!allowScreenshots)
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)

        // Run the default commands.
        super.onCreate(savedInstanceState)

        // Set the content view.
        if (bottomAppBar)
            setContentView(R.layout.import_export_bottom_appbar)
        else
            setContentView(R.layout.import_export_top_appbar)

        // Get a handle for the toolbar.
        val toolbar = findViewById<Toolbar>(R.id.import_export_toolbar)

        // Set the support action bar.
        setSupportActionBar(toolbar)

        // Get a handle for the action bar.
        val actionBar = supportActionBar!!

        // Display the home arrow on the support action bar.
        actionBar.setDisplayHomeAsUpEnabled(true)

        // Find out if OpenKeychain is installed.
        openKeychainInstalled = try {
            packageManager.getPackageInfo("org.sufficientlysecure.keychain", 0).versionName.isNotEmpty()
        } catch (exception: PackageManager.NameNotFoundException) {
            false
        }

        // Get handles for the views.
        encryptionSpinner = findViewById(R.id.encryption_spinner)
        encryptionPasswordTextInputLayout = findViewById(R.id.encryption_password_textinputlayout)
        encryptionPasswordEditText = findViewById(R.id.encryption_password_edittext)
        openKeychainRequiredTextView = findViewById(R.id.openkeychain_required_textview)
        fileLocationCardView = findViewById(R.id.file_location_cardview)
        importRadioButton = findViewById(R.id.import_radiobutton)
        val exportRadioButton = findViewById<RadioButton>(R.id.export_radiobutton)
        fileNameLinearLayout = findViewById(R.id.file_name_linearlayout)
        fileNameEditText = findViewById(R.id.file_name_edittext)
        openKeychainImportInstructionsTextView = findViewById(R.id.openkeychain_import_instructions_textview)
        importExportButton = findViewById(R.id.import_export_button)

        // Create an array adapter for the spinner.
        val encryptionArrayAdapter = ArrayAdapter.createFromResource(this, R.array.encryption_type, R.layout.spinner_item)

        // Set the drop down view resource on the spinner.
        encryptionArrayAdapter.setDropDownViewResource(R.layout.spinner_dropdown_items)

        // Set the array adapter for the spinner.
        encryptionSpinner.adapter = encryptionArrayAdapter

        // Update the UI when the spinner changes.
        encryptionSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View, position: Int, id: Long) {
                when (position) {
                    NO_ENCRYPTION -> {
                        // Hide the unneeded layout items.
                        encryptionPasswordTextInputLayout.visibility = View.GONE
                        openKeychainRequiredTextView.visibility = View.GONE
                        openKeychainImportInstructionsTextView.visibility = View.GONE

                        // Show the file location card.
                        fileLocationCardView.visibility = View.VISIBLE

                        // Show the file name linear layout if either import or export is checked.
                        if (importRadioButton.isChecked || exportRadioButton.isChecked)
                            fileNameLinearLayout.visibility = View.VISIBLE

                        // Reset the text of the import button, which may have been changed to `Decrypt`.
                        if (importRadioButton.isChecked)
                            importExportButton.setText(R.string.import_button)

                        // Clear the file name edit text.
                        fileNameEditText.text.clear()

                        // Disable the import/export button.
                        importExportButton.isEnabled = false
                    }

                    PASSWORD_ENCRYPTION -> {
                        // Hide the OpenPGP layout items.
                        openKeychainRequiredTextView.visibility = View.GONE
                        openKeychainImportInstructionsTextView.visibility = View.GONE

                        // Show the password encryption layout items.
                        encryptionPasswordTextInputLayout.visibility = View.VISIBLE

                        // Show the file location card.
                        fileLocationCardView.visibility = View.VISIBLE

                        // Show the file name linear layout if either import or export is checked.
                        if (importRadioButton.isChecked || exportRadioButton.isChecked)
                            fileNameLinearLayout.visibility = View.VISIBLE

                        // Reset the text of the import button, which may have been changed to `Decrypt`.
                        if (importRadioButton.isChecked)
                            importExportButton.setText(R.string.import_button)

                        // Clear the file name edit text.
                        fileNameEditText.text.clear()

                        // Disable the import/export button.
                        importExportButton.isEnabled = false
                    }

                    OPENPGP_ENCRYPTION -> {
                        // Hide the password encryption layout items.
                        encryptionPasswordTextInputLayout.visibility = View.GONE

                        // Updated items based on the installation status of OpenKeychain.
                        if (openKeychainInstalled) {  // OpenKeychain is installed.
                            // Show the file location card.
                            fileLocationCardView.visibility = View.VISIBLE

                            // Update the layout based on the checked radio button.
                            if (importRadioButton.isChecked) {
                                // Show the file name linear layout and the OpenKeychain import instructions.
                                fileNameLinearLayout.visibility = View.VISIBLE
                                openKeychainImportInstructionsTextView.visibility = View.VISIBLE

                                // Set the text of the import button to be `Decrypt`.
                                importExportButton.setText(R.string.decrypt)

                                // Clear the file name edit text.
                                fileNameEditText.text.clear()

                                // Disable the import/export button.
                                importExportButton.isEnabled = false
                            } else if (exportRadioButton.isChecked) {
                                // Hide the file name linear layout and the OpenKeychain import instructions.
                                fileNameLinearLayout.visibility = View.GONE
                                openKeychainImportInstructionsTextView.visibility = View.GONE

                                // Enable the export button.
                                importExportButton.isEnabled = true
                            }
                        } else {  // OpenKeychain is not installed.
                            // Show the OpenPGP required layout item.
                            openKeychainRequiredTextView.visibility = View.VISIBLE

                            // Hide the file location card.
                            fileLocationCardView.visibility = View.GONE
                        }
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Update the status of the import/export button when the password changes.
        encryptionPasswordEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
                // Do nothing.
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                // Do nothing.
            }

            override fun afterTextChanged(s: Editable) {
                // Enable the import/export button if both the file string and the password are populated.
                importExportButton.isEnabled = fileNameEditText.text.toString().isNotEmpty() && encryptionPasswordEditText.text.toString().isNotEmpty()
            }
        })

        // Update the UI when the file name edit text changes.
        fileNameEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
                // Do nothing.
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                // Do nothing.
            }

            override fun afterTextChanged(s: Editable) {
                // Adjust the UI according to the encryption spinner position.
                if (encryptionSpinner.selectedItemPosition == PASSWORD_ENCRYPTION) {
                    // Enable the import/export button if both the file name and the password are populated.
                    importExportButton.isEnabled = fileNameEditText.text.toString().isNotEmpty() && encryptionPasswordEditText.text.toString().isNotEmpty()
                } else {
                    // Enable the export button if the file name is populated.
                    importExportButton.isEnabled = fileNameEditText.text.toString().isNotEmpty()
                }
            }
        })

        // Check to see if the activity has been restarted.
        if (savedInstanceState == null) {  // The app has not been restarted.
            // Initially hide the unneeded views.
            encryptionPasswordTextInputLayout.visibility = View.GONE
            openKeychainRequiredTextView.visibility = View.GONE
            fileNameLinearLayout.visibility = View.GONE
            openKeychainImportInstructionsTextView.visibility = View.GONE
            importExportButton.visibility = View.GONE
        } else {  // The app has been restarted.
            // Restore the visibility of the views.
            encryptionPasswordTextInputLayout.visibility = savedInstanceState.getInt(ENCRYPTION_PASSWORD_TEXTINPUTLAYOUT_VISIBILITY)
            openKeychainRequiredTextView.visibility = savedInstanceState.getInt(OPEN_KEYCHAIN_REQUIRED_TEXTVIEW_VISIBILITY)
            fileLocationCardView.visibility = savedInstanceState.getInt(FILE_LOCATION_CARD_VIEW)
            fileNameLinearLayout.visibility = savedInstanceState.getInt(FILE_NAME_LINEARLAYOUT_VISIBILITY)
            openKeychainImportInstructionsTextView.visibility = savedInstanceState.getInt(OPEN_KEYCHAIN_IMPORT_INSTRUCTIONS_TEXTVIEW_VISIBILITY)
            importExportButton.visibility = savedInstanceState.getInt(IMPORT_EXPORT_BUTTON_VISIBILITY)

            // Restore the text.
            fileNameEditText.post { fileNameEditText.setText(savedInstanceState.getString(FILE_NAME_TEXT)) }
            importExportButton.text = savedInstanceState.getString(IMPORT_EXPORT_BUTTON_TEXT)
        }
    }

    public override fun onSaveInstanceState(savedInstanceState: Bundle) {
        // Run the default commands.
        super.onSaveInstanceState(savedInstanceState)

        // Save the visibility of the views.
        savedInstanceState.putInt(ENCRYPTION_PASSWORD_TEXTINPUTLAYOUT_VISIBILITY, encryptionPasswordTextInputLayout.visibility)
        savedInstanceState.putInt(OPEN_KEYCHAIN_REQUIRED_TEXTVIEW_VISIBILITY, openKeychainRequiredTextView.visibility)
        savedInstanceState.putInt(FILE_LOCATION_CARD_VIEW, fileLocationCardView.visibility)
        savedInstanceState.putInt(FILE_NAME_LINEARLAYOUT_VISIBILITY, fileNameLinearLayout.visibility)
        savedInstanceState.putInt(OPEN_KEYCHAIN_IMPORT_INSTRUCTIONS_TEXTVIEW_VISIBILITY, openKeychainImportInstructionsTextView.visibility)
        savedInstanceState.putInt(IMPORT_EXPORT_BUTTON_VISIBILITY, importExportButton.visibility)

        // Save the text.
        savedInstanceState.putString(FILE_NAME_TEXT, fileNameEditText.text.toString())
        savedInstanceState.putString(IMPORT_EXPORT_BUTTON_TEXT, importExportButton.text.toString())
    }

    fun onClickRadioButton(view: View) {
        // Check to see if import or export was selected.
        if (view.id == R.id.import_radiobutton) {  // The import radio button is selected.
            // Check to see if OpenPGP encryption is selected.
            if (encryptionSpinner.selectedItemPosition == OPENPGP_ENCRYPTION) {  // OpenPGP encryption selected.
                // Show the OpenKeychain import instructions.
                openKeychainImportInstructionsTextView.visibility = View.VISIBLE

                // Set the text on the import/export button to be `Decrypt`.
                importExportButton.setText(R.string.decrypt)
            } else {  // OpenPGP encryption not selected.
                // Hide the OpenKeychain import instructions.
                openKeychainImportInstructionsTextView.visibility = View.GONE

                // Set the text on the import/export button to be `Import`.
                importExportButton.setText(R.string.import_button)
            }

            // Display the file name views.
            fileNameLinearLayout.visibility = View.VISIBLE
            importExportButton.visibility = View.VISIBLE

            // Clear the file name edit text.
            fileNameEditText.text.clear()

            // Disable the import/export button.
            importExportButton.isEnabled = false
        } else {  // The export radio button is selected.
            // Hide the OpenKeychain import instructions.
            openKeychainImportInstructionsTextView.visibility = View.GONE

            // Set the text on the import/export button to be `Export`.
            importExportButton.setText(R.string.export)

            // Show the import/export button.
            importExportButton.visibility = View.VISIBLE

            // Check to see if OpenPGP encryption is selected.
            if (encryptionSpinner.selectedItemPosition == OPENPGP_ENCRYPTION) {  // OpenPGP encryption is selected.
                // Hide the file name views.
                fileNameLinearLayout.visibility = View.GONE

                // Enable the export button.
                importExportButton.isEnabled = true
            } else {  // OpenPGP encryption is not selected.
                // Show the file name view.
                fileNameLinearLayout.visibility = View.VISIBLE

                // Clear the file name edit text.
                fileNameEditText.text.clear()

                // Disable the import/export button.
                importExportButton.isEnabled = false
            }
        }
    }

    fun browse(@Suppress("UNUSED_PARAMETER") view: View) {
        // Check to see if import or export is selected.
        if (importRadioButton.isChecked) {  // Import is selected.
            // Open the file picker.
            browseForImportActivityResultLauncher.launch("*/*")
        } else {  // Export is selected
            // Open the file picker with the export name according to the encryption type.
            if (encryptionSpinner.selectedItemPosition == NO_ENCRYPTION)  // No encryption is selected.
                browseForExportActivityResultLauncher.launch(getString(R.string.privacy_browser_settings_pbs, BuildConfig.VERSION_NAME, IMPORT_EXPORT_SCHEMA_VERSION))
            else  // Password encryption is selected.
                browseForExportActivityResultLauncher.launch(getString(R.string.privacy_browser_settings_pbs_aes, BuildConfig.VERSION_NAME, IMPORT_EXPORT_SCHEMA_VERSION))
        }
    }

    fun importExport(@Suppress("UNUSED_PARAMETER") view: View) {
        // Instantiate the import export database helper.
        val importExportDatabaseHelper = ImportExportDatabaseHelper()

        // Check to see if import or export is selected.
        if (importRadioButton.isChecked) {  // Import is selected.
            // Initialize the import status string
            var importStatus = ""

            // Get the file name string.
            val fileNameString = fileNameEditText.text.toString()

            // Import according to the encryption type.
            when (encryptionSpinner.selectedItemPosition) {
                NO_ENCRYPTION -> {
                    try {
                        // Get an input stream for the file name.
                        // A file may be opened directly once the minimum API >= 29.  <https://developer.android.com/reference/kotlin/android/content/ContentResolver#openfile>
                        val inputStream = contentResolver.openInputStream(Uri.parse(fileNameString))!!

                        // Import the unencrypted file.
                        importStatus = importExportDatabaseHelper.importUnencrypted(inputStream, this)

                        // Close the input stream.
                        inputStream.close()
                    } catch (exception: FileNotFoundException) {
                        // Update the import status.
                        importStatus = exception.toString()
                    }

                    // Restart Privacy Browser if successful.
                    if (importStatus == IMPORT_SUCCESSFUL)
                        restartPrivacyBrowser()
                }

                PASSWORD_ENCRYPTION -> {
                    try {
                        // Get the encryption password.
                        val encryptionPasswordString = encryptionPasswordEditText.text.toString()

                        // Get an input stream for the file name.
                        val inputStream = contentResolver.openInputStream(Uri.parse(fileNameString))!!

                        // Initialize a salt byte array.  Salt protects against rainbow table attacks.
                        val saltByteArray = ByteArray(32)

                        // Get the salt from the beginning of the import file.
                        inputStream.read(saltByteArray)

                        // Create an initialization vector.
                        val initializationVector = ByteArray(12)

                        // Get the initialization vector from the import file.
                        inputStream.read(initializationVector)

                        // Convert the encryption password to a byte array.
                        val encryptionPasswordByteArray = encryptionPasswordString.toByteArray(StandardCharsets.UTF_8)

                        // Create an encryption password with salt byte array.
                        val encryptionPasswordWithSaltByteArray = ByteArray(encryptionPasswordByteArray.size + saltByteArray.size)

                        // Populate the first part of the encryption password with salt byte array with the encryption password.
                        System.arraycopy(encryptionPasswordByteArray, 0, encryptionPasswordWithSaltByteArray, 0, encryptionPasswordByteArray.size)

                        // Populate the second part of the encryption password with salt byte array with the salt.
                        System.arraycopy(saltByteArray, 0, encryptionPasswordWithSaltByteArray, encryptionPasswordByteArray.size, saltByteArray.size)

                        // Get a SHA-512 message digest.
                        val messageDigest = MessageDigest.getInstance("SHA-512")

                        // Hash the salted encryption password.  Otherwise, any characters after the 32nd character in the password are ignored.
                        val hashedEncryptionPasswordWithSaltByteArray = messageDigest.digest(encryptionPasswordWithSaltByteArray)

                        // Truncate the encryption password byte array to 256 bits (32 bytes).
                        val truncatedHashedEncryptionPasswordWithSaltByteArray = Arrays.copyOf(hashedEncryptionPasswordWithSaltByteArray, 32)

                        // Create an AES secret key from the encryption password byte array.
                        val secretKey = SecretKeySpec(truncatedHashedEncryptionPasswordWithSaltByteArray, "AES")

                        // Get a Advanced Encryption Standard, Galois/Counter Mode, No Padding cipher instance. Galois/Counter mode protects against modification of the ciphertext.  It doesn't use padding.
                        val cipher = Cipher.getInstance("AES/GCM/NoPadding")

                        // Set the GCM tag length to be 128 bits (the maximum) and apply the initialization vector.
                        val gcmParameterSpec = GCMParameterSpec(128, initializationVector)

                        // Initialize the cipher.
                        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmParameterSpec)

                        // Create a cipher input stream.
                        val cipherInputStream = CipherInputStream(inputStream, cipher)

                        // Initialize variables to store data as it is moved from the cipher input stream to the unencrypted import file output stream.  Move 128 bits (16 bytes) at a time.
                        var numberOfBytesRead: Int
                        val decryptedBytes = ByteArray(16)

                        // Create a private temporary unencrypted import file.
                        val temporaryUnencryptedImportFile = File.createTempFile("temporary_unencrypted_import_file", null, applicationContext.cacheDir)

                        // Create an temporary unencrypted import file output stream.
                        val temporaryUnencryptedImportFileOutputStream = FileOutputStream(temporaryUnencryptedImportFile)

                        // Read up to 128 bits (16 bytes) of data from the cipher input stream.  `-1` will be returned when the end of the file is reached.
                        while (cipherInputStream.read(decryptedBytes).also { numberOfBytesRead = it } != -1) {
                            // Write the data to the temporary unencrypted import file output stream.
                            temporaryUnencryptedImportFileOutputStream.write(decryptedBytes, 0, numberOfBytesRead)
                        }

                        // Flush the temporary unencrypted import file output stream.
                        temporaryUnencryptedImportFileOutputStream.flush()

                        // Close the streams.
                        temporaryUnencryptedImportFileOutputStream.close()
                        cipherInputStream.close()
                        inputStream.close()

                        // Wipe the encryption data from memory.
                        Arrays.fill(saltByteArray, 0.toByte())
                        Arrays.fill(initializationVector, 0.toByte())
                        Arrays.fill(encryptionPasswordByteArray, 0.toByte())
                        Arrays.fill(encryptionPasswordWithSaltByteArray, 0.toByte())
                        Arrays.fill(hashedEncryptionPasswordWithSaltByteArray, 0.toByte())
                        Arrays.fill(truncatedHashedEncryptionPasswordWithSaltByteArray, 0.toByte())
                        Arrays.fill(decryptedBytes, 0.toByte())

                        // Create a temporary unencrypted import file input stream.
                        val temporaryUnencryptedImportFileInputStream = FileInputStream(temporaryUnencryptedImportFile)

                        // Import the temporary unencrypted import file.
                        importStatus = importExportDatabaseHelper.importUnencrypted(temporaryUnencryptedImportFileInputStream, this)

                        // Close the temporary unencrypted import file input stream.
                        temporaryUnencryptedImportFileInputStream.close()

                        // Delete the temporary unencrypted import file.
                        temporaryUnencryptedImportFile.delete()

                        // Restart Privacy Browser if successful.
                        if (importStatus == IMPORT_SUCCESSFUL)
                            restartPrivacyBrowser()
                    } catch (exception: Exception) {
                        // Update the import status.
                        importStatus = exception.toString()
                    }
                }

                OPENPGP_ENCRYPTION -> {
                    try {
                        // Get a handle for the file provider directory.
                        fileProviderDirectory = File(applicationContext.cacheDir.toString() + "/" + getString(R.string.file_provider_directory))

                        // Create the file provider directory.  Any errors will be handled by the catch statement below.
                        fileProviderDirectory.mkdir()

                        // Set the temporary PGP encrypted import file.
                        temporaryPgpEncryptedImportFile = File.createTempFile("temporary_pgp_encrypted_import_file", null, fileProviderDirectory)

                        // Create a temporary PGP encrypted import file output stream.
                        val temporaryPgpEncryptedImportFileOutputStream = FileOutputStream(temporaryPgpEncryptedImportFile)

                        // Get an input stream for the file name.
                        val inputStream = contentResolver.openInputStream(Uri.parse(fileNameString))!!

                        // Create a transfer byte array.
                        val transferByteArray = ByteArray(1024)

                        // Create an integer to track the number of bytes read.
                        var bytesRead: Int

                        // Copy the input stream to the temporary PGP encrypted import file.
                        while (inputStream.read(transferByteArray).also { bytesRead = it } > 0)
                            temporaryPgpEncryptedImportFileOutputStream.write(transferByteArray, 0, bytesRead)

                        // Flush the temporary PGP encrypted import file output stream.
                        temporaryPgpEncryptedImportFileOutputStream.flush()

                        // Close the streams.
                        inputStream.close()
                        temporaryPgpEncryptedImportFileOutputStream.close()

                        // Create a decryption intent for OpenKeychain.
                        val openKeychainDecryptIntent = Intent("org.sufficientlysecure.keychain.action.DECRYPT_DATA")

                        // Include the URI to be decrypted.
                        openKeychainDecryptIntent.data = FileProvider.getUriForFile(this, getString(R.string.file_provider), temporaryPgpEncryptedImportFile)

                        // Allow OpenKeychain to read the file URI.
                        openKeychainDecryptIntent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION

                        // Send the intent to the OpenKeychain package.
                        openKeychainDecryptIntent.setPackage("org.sufficientlysecure.keychain")

                        // Make it so.
                        openKeychainDecryptActivityResultLauncher.launch(openKeychainDecryptIntent)

                        // Update the import status.
                        importStatus = IMPORT_SUCCESSFUL
                    } catch (exception: Exception) {
                        // Update the import status.
                        importStatus = exception.toString()
                    }
                }
            }

            // Display a snack bar with the import error if it was unsuccessful.
            if (importStatus != IMPORT_SUCCESSFUL)
                Snackbar.make(fileNameEditText, getString(R.string.import_failed, importStatus), Snackbar.LENGTH_INDEFINITE).show()
        } else {  // Export is selected.
            // Export according to the encryption type.
            when (encryptionSpinner.selectedItemPosition) {
                NO_ENCRYPTION -> {
                    // Get the file name string.
                    val noEncryptionFileNameString = fileNameEditText.text.toString()

                    try {
                        // Get the export file output stream.
                        // A file may be opened directly once the minimum API >= 29.  <https://developer.android.com/reference/kotlin/android/content/ContentResolver#openfile>
                        val exportFileOutputStream = contentResolver.openOutputStream(Uri.parse(noEncryptionFileNameString))!!

                        // Export the unencrypted file.
                        val noEncryptionExportStatus = importExportDatabaseHelper.exportUnencrypted(exportFileOutputStream, this)

                        // Close the output stream.
                        exportFileOutputStream.close()

                        // Display an export disposition snackbar.
                        if (noEncryptionExportStatus == EXPORT_SUCCESSFUL)
                            Snackbar.make(fileNameEditText, getString(R.string.export_successful), Snackbar.LENGTH_SHORT).show()
                        else
                            Snackbar.make(fileNameEditText, getString(R.string.export_failed, noEncryptionExportStatus), Snackbar.LENGTH_INDEFINITE).show()
                    } catch (fileNotFoundException: FileNotFoundException) {
                        // Display a snackbar with the exception.
                        Snackbar.make(fileNameEditText, getString(R.string.export_failed, fileNotFoundException), Snackbar.LENGTH_INDEFINITE).show()
                    }
                }

                PASSWORD_ENCRYPTION -> {
                    try {
                        // Create a temporary unencrypted export file.
                        val temporaryUnencryptedExportFile = File.createTempFile("temporary_unencrypted_export_file", null, applicationContext.cacheDir)

                        // Create a temporary unencrypted export output stream.
                        val temporaryUnencryptedExportOutputStream = FileOutputStream(temporaryUnencryptedExportFile)

                        // Populate the temporary unencrypted export.
                        val passwordEncryptionExportStatus = importExportDatabaseHelper.exportUnencrypted(temporaryUnencryptedExportOutputStream, this)

                        // Close the temporary unencrypted export output stream.
                        temporaryUnencryptedExportOutputStream.close()

                        // Create an unencrypted export file input stream.
                        val unencryptedExportFileInputStream = FileInputStream(temporaryUnencryptedExportFile)

                        // Get the encryption password.
                        val encryptionPasswordString = encryptionPasswordEditText.text.toString()

                        // Initialize a secure random number generator.
                        val secureRandom = SecureRandom()

                        // Initialize a salt byte array.  Salt protects against rainbow table attacks.
                        val saltByteArray = ByteArray(32)

                        // Get a 256 bit (32 byte) random salt.
                        secureRandom.nextBytes(saltByteArray)

                        // Convert the encryption password to a byte array.
                        val encryptionPasswordByteArray = encryptionPasswordString.toByteArray(StandardCharsets.UTF_8)

                        // Create an encryption password with salt byte array.
                        val encryptionPasswordWithSaltByteArray = ByteArray(encryptionPasswordByteArray.size + saltByteArray.size)

                        // Populate the first part of the encryption password with salt byte array with the encryption password.
                        System.arraycopy(encryptionPasswordByteArray, 0, encryptionPasswordWithSaltByteArray, 0, encryptionPasswordByteArray.size)

                        // Populate the second part of the encryption password with salt byte array with the salt.
                        System.arraycopy(saltByteArray, 0, encryptionPasswordWithSaltByteArray, encryptionPasswordByteArray.size, saltByteArray.size)

                        // Get a SHA-512 message digest.
                        val messageDigest = MessageDigest.getInstance("SHA-512")

                        // Hash the salted encryption password.  Otherwise, any characters after the 32nd character in the password are ignored.
                        val hashedEncryptionPasswordWithSaltByteArray = messageDigest.digest(encryptionPasswordWithSaltByteArray)

                        // Truncate the encryption password byte array to 256 bits (32 bytes).
                        val truncatedHashedEncryptionPasswordWithSaltByteArray = Arrays.copyOf(hashedEncryptionPasswordWithSaltByteArray, 32)

                        // Create an AES secret key from the encryption password byte array.
                        val secretKey = SecretKeySpec(truncatedHashedEncryptionPasswordWithSaltByteArray, "AES")

                        // Create an initialization vector.  According to NIST, a 12 byte initialization vector is more secure than a 16 byte one.
                        val initializationVector = ByteArray(12)

                        // Populate the initialization vector with random data.
                        secureRandom.nextBytes(initializationVector)

                        // Get a Advanced Encryption Standard, Galois/Counter Mode, No Padding cipher instance. Galois/Counter mode protects against modification of the ciphertext.  It doesn't use padding.
                        val cipher = Cipher.getInstance("AES/GCM/NoPadding")

                        // Set the GCM tag length to be 128 bits (the maximum) and apply the initialization vector.
                        val gcmParameterSpec = GCMParameterSpec(128, initializationVector)

                        // Initialize the cipher.
                        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmParameterSpec)

                        // Get the file name string.
                        val passwordEncryptionFileNameString = fileNameEditText.text.toString()

                        // Get the export file output stream.
                        val exportFileOutputStream = contentResolver.openOutputStream(Uri.parse(passwordEncryptionFileNameString))!!

                        // Add the salt and the initialization vector to the export file output stream.
                        exportFileOutputStream.write(saltByteArray)
                        exportFileOutputStream.write(initializationVector)

                        // Create a cipher output stream.
                        val cipherOutputStream = CipherOutputStream(exportFileOutputStream, cipher)

                        // Initialize variables to store data as it is moved from the unencrypted export file input stream to the cipher output stream.  Move 128 bits (16 bytes) at a time.
                        var numberOfBytesRead: Int
                        val encryptedBytes = ByteArray(16)

                        // Read up to 128 bits (16 bytes) of data from the unencrypted export file stream.  `-1` will be returned when the end of the file is reached.
                        while (unencryptedExportFileInputStream.read(encryptedBytes).also { numberOfBytesRead = it } != -1)
                        // Write the data to the cipher output stream.
                            cipherOutputStream.write(encryptedBytes, 0, numberOfBytesRead)

                        // Close the streams.
                        cipherOutputStream.flush()
                        cipherOutputStream.close()
                        exportFileOutputStream.close()
                        unencryptedExportFileInputStream.close()

                        // Wipe the encryption data from memory.
                        Arrays.fill(saltByteArray, 0.toByte())
                        Arrays.fill(encryptionPasswordByteArray, 0.toByte())
                        Arrays.fill(encryptionPasswordWithSaltByteArray, 0.toByte())
                        Arrays.fill(hashedEncryptionPasswordWithSaltByteArray, 0.toByte())
                        Arrays.fill(truncatedHashedEncryptionPasswordWithSaltByteArray, 0.toByte())
                        Arrays.fill(initializationVector, 0.toByte())
                        Arrays.fill(encryptedBytes, 0.toByte())

                        // Delete the temporary unencrypted export file.
                        temporaryUnencryptedExportFile.delete()

                        // Display an export disposition snackbar.
                        if (passwordEncryptionExportStatus == EXPORT_SUCCESSFUL)
                            Snackbar.make(fileNameEditText, getString(R.string.export_successful), Snackbar.LENGTH_SHORT).show()
                        else
                            Snackbar.make(fileNameEditText, getString(R.string.export_failed, passwordEncryptionExportStatus), Snackbar.LENGTH_INDEFINITE).show()
                    } catch (exception: Exception) {
                        // Display a snackbar with the exception.
                        Snackbar.make(fileNameEditText, getString(R.string.export_failed, exception), Snackbar.LENGTH_INDEFINITE).show()
                    }
                }

                OPENPGP_ENCRYPTION -> {
                    try {
                        // Get a handle for the file provider directory.
                        fileProviderDirectory = File(applicationContext.cacheDir.toString() + "/" + getString(R.string.file_provider_directory))

                        // Create the file provider directory.  Any errors will be handled by the catch statement below.
                        fileProviderDirectory.mkdir()

                        // Set the temporary pre-encrypted export file.
                        temporaryPreEncryptedExportFile = File(fileProviderDirectory.toString() + "/" +
                                getString(R.string.privacy_browser_settings_pbs, BuildConfig.VERSION_NAME, IMPORT_EXPORT_SCHEMA_VERSION))

                        // Delete the temporary pre-encrypted export file if it already exists.
                        if (temporaryPreEncryptedExportFile.exists())
                            temporaryPreEncryptedExportFile.delete()

                        // Create a temporary pre-encrypted export output stream.
                        val temporaryPreEncryptedExportOutputStream = FileOutputStream(temporaryPreEncryptedExportFile)

                        // Populate the temporary pre-encrypted export file.
                        val openpgpEncryptionExportStatus = importExportDatabaseHelper.exportUnencrypted(temporaryPreEncryptedExportOutputStream, this)

                        // Flush the temporary pre-encryption export output stream.
                        temporaryPreEncryptedExportOutputStream.flush()

                        // Close the temporary pre-encryption export output stream.
                        temporaryPreEncryptedExportOutputStream.close()

                        // Display an export error snackbar if the temporary pre-encrypted export failed.
                        if (openpgpEncryptionExportStatus != EXPORT_SUCCESSFUL)
                            Snackbar.make(fileNameEditText, getString(R.string.export_failed, openpgpEncryptionExportStatus), Snackbar.LENGTH_INDEFINITE).show()

                        // Create an encryption intent for OpenKeychain.
                        val openKeychainEncryptIntent = Intent("org.sufficientlysecure.keychain.action.ENCRYPT_DATA")

                        // Include the temporary unencrypted export file URI.
                        openKeychainEncryptIntent.data = FileProvider.getUriForFile(this, getString(R.string.file_provider), temporaryPreEncryptedExportFile)

                        // Allow OpenKeychain to read the file URI.
                        openKeychainEncryptIntent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION

                        // Send the intent to the OpenKeychain package.
                        openKeychainEncryptIntent.setPackage("org.sufficientlysecure.keychain")

                        // Make it so.
                        openKeychainEncryptActivityResultLauncher.launch(openKeychainEncryptIntent)
                    } catch (exception: Exception) {
                        // Display a snackbar with the exception.
                        Snackbar.make(fileNameEditText, getString(R.string.export_failed, exception), Snackbar.LENGTH_INDEFINITE).show()
                    }
                }
            }
        }
    }

    private fun restartPrivacyBrowser() {
        // Create an intent to restart Privacy Browser.
        val restartIntent = parentActivityIntent!!

        // `Intent.FLAG_ACTIVITY_CLEAR_TASK` removes all activities from the stack.  It requires `Intent.FLAG_ACTIVITY_NEW_TASK`.
        restartIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

        // Create a restart handler.
        val restartHandler = Handler(mainLooper)

        // Create a restart runnable.
        val restartRunnable = Runnable {

            // Restart Privacy Browser.
            startActivity(restartIntent)

            // Kill this instance of Privacy Browser.  Otherwise, the app exhibits sporadic behavior after the restart.
            exitProcess(0)
        }

        // Restart Privacy Browser after 150 milliseconds to allow enough time for the preferences to be saved.
        restartHandler.postDelayed(restartRunnable, 150)
    }
}
