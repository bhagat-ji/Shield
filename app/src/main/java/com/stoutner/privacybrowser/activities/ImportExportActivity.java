/*
 * Copyright © 2018-2021 Soren Stoutner <soren@stoutner.com>.
 *
 * This file is part of Privacy Browser <https://www.stoutner.com/privacy-browser>.
 *
 * Privacy Browser is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Privacy Browser is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Privacy Browser.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.stoutner.privacybrowser.activities;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.content.FileProvider;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;

import com.stoutner.privacybrowser.BuildConfig;
import com.stoutner.privacybrowser.R;
import com.stoutner.privacybrowser.helpers.ImportExportDatabaseHelper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class ImportExportActivity extends AppCompatActivity {
    // Define the encryption constants.
    private final int NO_ENCRYPTION = 0;
    private final int PASSWORD_ENCRYPTION = 1;
    private final int OPENPGP_ENCRYPTION = 2;

    // Define the activity result constants.
    private final int BROWSE_RESULT_CODE = 0;
    private final int OPENPGP_IMPORT_RESULT_CODE = 1;
    private final int OPENPGP_EXPORT_RESULT_CODE = 2;

    // Define the saved instance state constants.
    private final String ENCRYPTION_PASSWORD_TEXTINPUTLAYOUT_VISIBILITY = "encryption_password_textinputlayout_visibility";
    private final String KITKAT_PASSWORD_ENCRYPTED_TEXTVIEW_VISIBILITY = "kitkat_password_encrypted_textview_visibility";
    private final String OPEN_KEYCHAIN_REQUIRED_TEXTVIEW_VISIBILITY = "open_keychain_required_textview_visibility";
    private final String FILE_LOCATION_CARD_VIEW = "file_location_card_view";
    private final String FILE_NAME_LINEARLAYOUT_VISIBILITY = "file_name_linearlayout_visibility";
    private final String OPEN_KEYCHAIN_IMPORT_INSTRUCTIONS_TEXTVIEW_VISIBILITY = "open_keychain_import_instructions_textview_visibility";
    private final String IMPORT_EXPORT_BUTTON_VISIBILITY = "import_export_button_visibility";
    private final String FILE_NAME_TEXT = "file_name_text";
    private final String IMPORT_EXPORT_BUTTON_TEXT = "import_export_button_text";

    // Define the class views.
    Spinner encryptionSpinner;
    TextInputLayout encryptionPasswordTextInputLayout;
    EditText encryptionPasswordEditText;
    TextView kitKatPasswordEncryptionTextView;
    TextView openKeychainRequiredTextView;
    CardView fileLocationCardView;
    RadioButton importRadioButton;
    LinearLayout fileNameLinearLayout;
    EditText fileNameEditText;
    TextView openKeychainImportInstructionsTextView;
    Button importExportButton;

    // Define the class variables.
    private boolean openKeychainInstalled;
    private File temporaryPgpEncryptedImportFile;
    private File temporaryPreEncryptedExportFile;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        // Get a handle for the shared preferences.
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        // Get the preferences.
        boolean allowScreenshots = sharedPreferences.getBoolean(getString(R.string.allow_screenshots_key), false);
        boolean bottomAppBar = sharedPreferences.getBoolean(getString(R.string.bottom_app_bar_key), false);

        // Disable screenshots if not allowed.
        if (!allowScreenshots) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        }

        // Set the theme.
        setTheme(R.style.PrivacyBrowser);

        // Run the default commands.
        super.onCreate(savedInstanceState);

        // Set the content view.
        if (bottomAppBar) {
            setContentView(R.layout.import_export_bottom_appbar);
        } else {
            setContentView(R.layout.import_export_top_appbar);
        }

        // Get a handle for the toolbar.
        Toolbar toolbar = findViewById(R.id.import_export_toolbar);

        // Set the support action bar.
        setSupportActionBar(toolbar);

        // Get a handle for the action bar.
        ActionBar actionBar = getSupportActionBar();

        // Remove the incorrect lint warning that the action bar might be null.
        assert actionBar != null;

        // Display the home arrow on the support action bar.
        actionBar.setDisplayHomeAsUpEnabled(true);

        // Find out if OpenKeychain is installed.
        try {
            openKeychainInstalled = !getPackageManager().getPackageInfo("org.sufficientlysecure.keychain", 0).versionName.isEmpty();
        } catch (PackageManager.NameNotFoundException exception) {
            openKeychainInstalled = false;
        }

        // Get handles for the views that need to be modified.
        encryptionSpinner = findViewById(R.id.encryption_spinner);
        encryptionPasswordTextInputLayout = findViewById(R.id.encryption_password_textinputlayout);
        encryptionPasswordEditText = findViewById(R.id.encryption_password_edittext);
        openKeychainRequiredTextView = findViewById(R.id.openkeychain_required_textview);
        fileLocationCardView = findViewById(R.id.file_location_cardview);
        importRadioButton = findViewById(R.id.import_radiobutton);
        RadioButton exportRadioButton = findViewById(R.id.export_radiobutton);
        fileNameLinearLayout = findViewById(R.id.file_name_linearlayout);
        fileNameEditText = findViewById(R.id.file_name_edittext);
        openKeychainImportInstructionsTextView = findViewById(R.id.openkeychain_import_instructions_textview);
        importExportButton = findViewById(R.id.import_export_button);

        // Create an array adapter for the spinner.
        ArrayAdapter<CharSequence> encryptionArrayAdapter = ArrayAdapter.createFromResource(this, R.array.encryption_type, R.layout.spinner_item);

        // Set the drop down view resource on the spinner.
        encryptionArrayAdapter.setDropDownViewResource(R.layout.spinner_dropdown_items);

        // Set the array adapter for the spinner.
        encryptionSpinner.setAdapter(encryptionArrayAdapter);

        // Update the UI when the spinner changes.
        encryptionSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case NO_ENCRYPTION:
                        // Hide the unneeded layout items.
                        encryptionPasswordTextInputLayout.setVisibility(View.GONE);
                        kitKatPasswordEncryptionTextView.setVisibility(View.GONE);
                        openKeychainRequiredTextView.setVisibility(View.GONE);
                        openKeychainImportInstructionsTextView.setVisibility(View.GONE);

                        // Show the file location card.
                        fileLocationCardView.setVisibility(View.VISIBLE);

                        // Show the file name linear layout if either import or export is checked.
                        if (importRadioButton.isChecked() || exportRadioButton.isChecked()) {
                            fileNameLinearLayout.setVisibility(View.VISIBLE);
                        }

                        // Reset the text of the import button, which may have been changed to `Decrypt`.
                        if (importRadioButton.isChecked()) {
                            importExportButton.setText(R.string.import_button);
                        }

                        // Enable the import/export button if the file name is populated.
                        importExportButton.setEnabled(!fileNameEditText.getText().toString().isEmpty());
                        break;

                    case PASSWORD_ENCRYPTION:
                        // Hide the OpenPGP layout items.
                        openKeychainRequiredTextView.setVisibility(View.GONE);
                        openKeychainImportInstructionsTextView.setVisibility(View.GONE);

                        // Show the password encryption layout items.
                        encryptionPasswordTextInputLayout.setVisibility(View.VISIBLE);

                        // Show the file location card.
                        fileLocationCardView.setVisibility(View.VISIBLE);

                        // Show the file name linear layout if either import or export is checked.
                        if (importRadioButton.isChecked() || exportRadioButton.isChecked()) {
                            fileNameLinearLayout.setVisibility(View.VISIBLE);
                        }

                        // Reset the text of the import button, which may have been changed to `Decrypt`.
                        if (importRadioButton.isChecked()) {
                            importExportButton.setText(R.string.import_button);
                        }

                        // Enable the import/button if both the password and the file name are populated.
                        importExportButton.setEnabled(!fileNameEditText.getText().toString().isEmpty() && !encryptionPasswordEditText.getText().toString().isEmpty());
                        break;

                    case OPENPGP_ENCRYPTION:
                        // Hide the password encryption layout items.
                        encryptionPasswordTextInputLayout.setVisibility(View.GONE);
                        kitKatPasswordEncryptionTextView.setVisibility(View.GONE);

                        // Updated items based on the installation status of OpenKeychain.
                        if (openKeychainInstalled) {  // OpenKeychain is installed.
                            // Show the file location card.
                            fileLocationCardView.setVisibility(View.VISIBLE);

                            if (importRadioButton.isChecked()) {
                                // Show the file name linear layout and the OpenKeychain import instructions.
                                fileNameLinearLayout.setVisibility(View.VISIBLE);
                                openKeychainImportInstructionsTextView.setVisibility(View.VISIBLE);

                                // Set the text of the import button to be `Decrypt`.
                                importExportButton.setText(R.string.decrypt);

                                // Enable the import button if the file name is populated.
                                importExportButton.setEnabled(!fileNameEditText.getText().toString().isEmpty());
                            } else if (exportRadioButton.isChecked()) {
                                // Hide the file name linear layout and the OpenKeychain import instructions.
                                fileNameLinearLayout.setVisibility(View.GONE);
                                openKeychainImportInstructionsTextView.setVisibility(View.GONE);

                                // Enable the export button.
                                importExportButton.setEnabled(true);
                            }
                        } else {  // OpenKeychain is not installed.
                            // Show the OpenPGP required layout item.
                            openKeychainRequiredTextView.setVisibility(View.VISIBLE);

                            // Hide the file location card.
                            fileLocationCardView.setVisibility(View.GONE);
                        }
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        // Update the status of the import/export button when the password changes.
        encryptionPasswordEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Do nothing.
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Do nothing.
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Enable the import/export button if both the file string and the password are populated.
                importExportButton.setEnabled(!fileNameEditText.getText().toString().isEmpty() && !encryptionPasswordEditText.getText().toString().isEmpty());
            }
        });

        // Update the UI when the file name EditText changes.
        fileNameEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Do nothing.
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Do nothing.
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Adjust the UI according to the encryption spinner position.
                if (encryptionSpinner.getSelectedItemPosition() == PASSWORD_ENCRYPTION) {
                    // Enable the import/export button if both the file name and the password are populated.
                    importExportButton.setEnabled(!fileNameEditText.getText().toString().isEmpty() && !encryptionPasswordEditText.getText().toString().isEmpty());
                } else {
                    // Enable the export button if the file name is populated.
                    importExportButton.setEnabled(!fileNameEditText.getText().toString().isEmpty());
                }
            }
        });

        // Check to see if the activity has been restarted.
        if (savedInstanceState == null) {  // The app has not been restarted.
            // Initially hide the unneeded views.
            encryptionPasswordTextInputLayout.setVisibility(View.GONE);
            kitKatPasswordEncryptionTextView.setVisibility(View.GONE);
            openKeychainRequiredTextView.setVisibility(View.GONE);
            fileNameLinearLayout.setVisibility(View.GONE);
            openKeychainImportInstructionsTextView.setVisibility(View.GONE);
            importExportButton.setVisibility(View.GONE);
        } else {  // The app has been restarted.
            // Restore the visibility of the views.
            encryptionPasswordTextInputLayout.setVisibility(savedInstanceState.getInt(ENCRYPTION_PASSWORD_TEXTINPUTLAYOUT_VISIBILITY));
            kitKatPasswordEncryptionTextView.setVisibility(savedInstanceState.getInt(KITKAT_PASSWORD_ENCRYPTED_TEXTVIEW_VISIBILITY));
            openKeychainRequiredTextView.setVisibility(savedInstanceState.getInt(OPEN_KEYCHAIN_REQUIRED_TEXTVIEW_VISIBILITY));
            fileLocationCardView.setVisibility(savedInstanceState.getInt(FILE_LOCATION_CARD_VIEW));
            fileNameLinearLayout.setVisibility(savedInstanceState.getInt(FILE_NAME_LINEARLAYOUT_VISIBILITY));
            openKeychainImportInstructionsTextView.setVisibility(savedInstanceState.getInt(OPEN_KEYCHAIN_IMPORT_INSTRUCTIONS_TEXTVIEW_VISIBILITY));
            importExportButton.setVisibility(savedInstanceState.getInt(IMPORT_EXPORT_BUTTON_VISIBILITY));

            // Restore the text.
            fileNameEditText.post(() -> fileNameEditText.setText(savedInstanceState.getString(FILE_NAME_TEXT)));
            importExportButton.setText(savedInstanceState.getString(IMPORT_EXPORT_BUTTON_TEXT));
        }
    }

    @Override
    public void onSaveInstanceState (@NonNull Bundle savedInstanceState) {
        // Run the default commands.
        super.onSaveInstanceState(savedInstanceState);

        // Save the visibility of the views.
        savedInstanceState.putInt(ENCRYPTION_PASSWORD_TEXTINPUTLAYOUT_VISIBILITY, encryptionPasswordTextInputLayout.getVisibility());
        savedInstanceState.putInt(KITKAT_PASSWORD_ENCRYPTED_TEXTVIEW_VISIBILITY, kitKatPasswordEncryptionTextView.getVisibility());
        savedInstanceState.putInt(OPEN_KEYCHAIN_REQUIRED_TEXTVIEW_VISIBILITY, openKeychainRequiredTextView.getVisibility());
        savedInstanceState.putInt(FILE_LOCATION_CARD_VIEW, fileLocationCardView.getVisibility());
        savedInstanceState.putInt(FILE_NAME_LINEARLAYOUT_VISIBILITY, fileNameLinearLayout.getVisibility());
        savedInstanceState.putInt(OPEN_KEYCHAIN_IMPORT_INSTRUCTIONS_TEXTVIEW_VISIBILITY, openKeychainImportInstructionsTextView.getVisibility());
        savedInstanceState.putInt(IMPORT_EXPORT_BUTTON_VISIBILITY, importExportButton.getVisibility());

        // Save the text.
        savedInstanceState.putString(FILE_NAME_TEXT, fileNameEditText.getText().toString());
        savedInstanceState.putString(IMPORT_EXPORT_BUTTON_TEXT, importExportButton.getText().toString());
    }

    public void onClickRadioButton(View view) {
        // Get the current file name.
        String fileNameString = fileNameEditText.getText().toString();

        // Convert the file name string to a file.
        File file = new File(fileNameString);

        // Check to see if import or export was selected.
        if (view.getId() == R.id.import_radiobutton) {  // The import radio button is selected.
            // Check to see if OpenPGP encryption is selected.
            if (encryptionSpinner.getSelectedItemPosition() == OPENPGP_ENCRYPTION) {  // OpenPGP encryption selected.
                // Show the OpenKeychain import instructions.
                openKeychainImportInstructionsTextView.setVisibility(View.VISIBLE);

                // Set the text on the import/export button to be `Decrypt`.
                importExportButton.setText(R.string.decrypt);
            } else {  // OpenPGP encryption not selected.
                // Hide the OpenKeychain import instructions.
                openKeychainImportInstructionsTextView.setVisibility(View.GONE);

                // Set the text on the import/export button to be `Import`.
                importExportButton.setText(R.string.import_button);
            }

            // Display the file name views.
            fileNameLinearLayout.setVisibility(View.VISIBLE);
            importExportButton.setVisibility(View.VISIBLE);

            // Check to see if the file exists.
            if (file.exists()) {  // The file exists.
                // Check to see if password encryption is selected.
                if (encryptionSpinner.getSelectedItemPosition() == PASSWORD_ENCRYPTION) {  // Password encryption is selected.
                    // Enable the import button if the encryption password is populated.
                    importExportButton.setEnabled(!encryptionPasswordEditText.getText().toString().isEmpty());
                } else {  // Password encryption is not selected.
                    // Enable the import/decrypt button.
                    importExportButton.setEnabled(true);
                }
            } else {  // The file does not exist.
                // Disable the import/decrypt button.
                importExportButton.setEnabled(false);
            }
        } else {  // The export radio button is selected.
            // Hide the OpenKeychain import instructions.
            openKeychainImportInstructionsTextView.setVisibility(View.GONE);

            // Set the text on the import/export button to be `Export`.
            importExportButton.setText(R.string.export);

            // Show the import/export button.
            importExportButton.setVisibility(View.VISIBLE);

            // Check to see if OpenPGP encryption is selected.
            if (encryptionSpinner.getSelectedItemPosition() == OPENPGP_ENCRYPTION) {  // OpenPGP encryption is selected.
                // Hide the file name views.
                fileNameLinearLayout.setVisibility(View.GONE);

                // Enable the export button.
                importExportButton.setEnabled(true);
            } else {  // OpenPGP encryption is not selected.
                // Show the file name view.
                fileNameLinearLayout.setVisibility(View.VISIBLE);

                // Check the encryption type.
                if (encryptionSpinner.getSelectedItemPosition() == NO_ENCRYPTION) {  // No encryption is selected.
                    // Enable the export button if the file name is populated.
                    importExportButton.setEnabled(!fileNameString.isEmpty());
                } else {  // Password encryption is selected.
                    // Enable the export button if the file name and the password are populated.
                    importExportButton.setEnabled(!fileNameString.isEmpty() && !encryptionPasswordEditText.getText().toString().isEmpty());
                }
            }
        }
    }

    public void browse(View view) {
        // Check to see if import or export is selected.
        if (importRadioButton.isChecked()) {  // Import is selected.
            // Create the file picker intent.
            Intent importBrowseIntent = new Intent(Intent.ACTION_OPEN_DOCUMENT);

            // Set the intent MIME type to include all files so that everything is visible.
            importBrowseIntent.setType("*/*");

            // Request a file that can be opened.
            importBrowseIntent.addCategory(Intent.CATEGORY_OPENABLE);

            // Launch the file picker.
            startActivityForResult(importBrowseIntent, BROWSE_RESULT_CODE);
        } else {  // Export is selected
            // Create the file picker intent.
            Intent exportBrowseIntent = new Intent(Intent.ACTION_CREATE_DOCUMENT);

            // Set the intent MIME type to include all files so that everything is visible.
            exportBrowseIntent.setType("*/*");

            // Set the initial export file name according to the encryption type.
            if (encryptionSpinner.getSelectedItemPosition() == NO_ENCRYPTION) {  // No encryption is selected.
                exportBrowseIntent.putExtra(Intent.EXTRA_TITLE, getString(R.string.settings) + " " + BuildConfig.VERSION_NAME + ".pbs");
            } else {  // Password encryption is selected.
                exportBrowseIntent.putExtra(Intent.EXTRA_TITLE, getString(R.string.settings) + " " + BuildConfig.VERSION_NAME + ".pbs.aes");
            }

            // Request a file that can be opened.
            exportBrowseIntent.addCategory(Intent.CATEGORY_OPENABLE);

            // Launch the file picker.
            startActivityForResult(exportBrowseIntent, BROWSE_RESULT_CODE);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent returnedIntent) {
        // Run the default commands.
        super.onActivityResult(requestCode, resultCode, returnedIntent);

        switch (requestCode) {
            case (BROWSE_RESULT_CODE):
                // Only do something if the user didn't press back from the file picker.
                if (resultCode == Activity.RESULT_OK) {
                    // Get the file path URI from the intent.
                    Uri fileNameUri = returnedIntent.getData();

                    // Get the file name string from the URI.
                    String fileNameString = fileNameUri.toString();

                    // Set the file name name text.
                    fileNameEditText.setText(fileNameString);

                    // Move the cursor to the end of the file name edit text.
                    fileNameEditText.setSelection(fileNameString.length());
                }
                break;

            case OPENPGP_IMPORT_RESULT_CODE:
                // Delete the temporary PGP encrypted import file.
                if (temporaryPgpEncryptedImportFile.exists()) {
                    //noinspection ResultOfMethodCallIgnored
                    temporaryPgpEncryptedImportFile.delete();
                }
                break;

            case OPENPGP_EXPORT_RESULT_CODE:
                // Delete the temporary pre-encrypted export file if it exists.
                if (temporaryPreEncryptedExportFile.exists()) {
                    //noinspection ResultOfMethodCallIgnored
                    temporaryPreEncryptedExportFile.delete();
                }
                break;
        }
    }

    public void importExport(View view) {
        // Instantiate the import export database helper.
        ImportExportDatabaseHelper importExportDatabaseHelper = new ImportExportDatabaseHelper();

        // Check to see if import or export is selected.
        if (importRadioButton.isChecked()) {  // Import is selected.
            // Initialize the import status string
            String importStatus = "";

            // Get the file name string.
            String fileNameString = fileNameEditText.getText().toString();

            // Import according to the encryption type.
            switch (encryptionSpinner.getSelectedItemPosition()) {
                case NO_ENCRYPTION:
                    try {
                        // Get an input stream for the file name.
                        InputStream inputStream = getContentResolver().openInputStream(Uri.parse(fileNameString));

                        // Import the unencrypted file.
                        importStatus = importExportDatabaseHelper.importUnencrypted(inputStream, this);
                    } catch (FileNotFoundException exception) {
                        // Update the import status.
                        importStatus = exception.toString();
                    }

                    // Restart Privacy Browser if successful.
                    if (importStatus.equals(ImportExportDatabaseHelper.IMPORT_SUCCESSFUL)) {
                        restartPrivacyBrowser();
                    }
                    break;

                case PASSWORD_ENCRYPTION:
                    try {
                        // Get the encryption password.
                        String encryptionPasswordString = encryptionPasswordEditText.getText().toString();

                        // Get an input stream for the file name.
                        InputStream inputStream = getContentResolver().openInputStream(Uri.parse(fileNameString));

                        // Get the salt from the beginning of the import file.
                        byte[] saltByteArray = new byte[32];
                        //noinspection ResultOfMethodCallIgnored
                        inputStream.read(saltByteArray);

                        // Get the initialization vector from the import file.
                        byte[] initializationVector = new byte[12];
                        //noinspection ResultOfMethodCallIgnored
                        inputStream.read(initializationVector);

                        // Convert the encryption password to a byte array.
                        byte[] encryptionPasswordByteArray = encryptionPasswordString.getBytes(StandardCharsets.UTF_8);

                        // Append the salt to the encryption password byte array.  This protects against rainbow table attacks.
                        byte[] encryptionPasswordWithSaltByteArray = new byte[encryptionPasswordByteArray.length + saltByteArray.length];
                        System.arraycopy(encryptionPasswordByteArray, 0, encryptionPasswordWithSaltByteArray, 0, encryptionPasswordByteArray.length);
                        System.arraycopy(saltByteArray, 0, encryptionPasswordWithSaltByteArray, encryptionPasswordByteArray.length, saltByteArray.length);

                        // Get a SHA-512 message digest.
                        MessageDigest messageDigest = MessageDigest.getInstance("SHA-512");

                        // Hash the salted encryption password.  Otherwise, any characters after the 32nd character in the password are ignored.
                        byte[] hashedEncryptionPasswordWithSaltByteArray = messageDigest.digest(encryptionPasswordWithSaltByteArray);

                        // Truncate the encryption password byte array to 256 bits (32 bytes).
                        byte[] truncatedHashedEncryptionPasswordWithSaltByteArray = Arrays.copyOf(hashedEncryptionPasswordWithSaltByteArray, 32);

                        // Create an AES secret key from the encryption password byte array.
                        SecretKeySpec secretKey = new SecretKeySpec(truncatedHashedEncryptionPasswordWithSaltByteArray, "AES");

                        // Get a Advanced Encryption Standard, Galois/Counter Mode, No Padding cipher instance. Galois/Counter mode protects against modification of the ciphertext.  It doesn't use padding.
                        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");

                        // Set the GCM tag length to be 128 bits (the maximum) and apply the initialization vector.
                        GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(128, initializationVector);

                        // Initialize the cipher.
                        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmParameterSpec);

                        // Create a cipher input stream.
                        CipherInputStream cipherInputStream = new CipherInputStream(inputStream, cipher);

                        // Initialize variables to store data as it is moved from the cipher input stream to the unencrypted import file output stream.  Move 128 bits (16 bytes) at a time.
                        int numberOfBytesRead;
                        byte[] decryptedBytes = new byte[16];


                        // Create a private temporary unencrypted import file.
                        File temporaryUnencryptedImportFile = File.createTempFile("temporary_unencrypted_import_file", null, getApplicationContext().getCacheDir());

                        // Create an temporary unencrypted import file output stream.
                        FileOutputStream temporaryUnencryptedImportFileOutputStream = new FileOutputStream(temporaryUnencryptedImportFile);


                        // Read up to 128 bits (16 bytes) of data from the cipher input stream.  `-1` will be returned when the end fo the file is reached.
                        while ((numberOfBytesRead = cipherInputStream.read(decryptedBytes)) != -1) {
                            // Write the data to the temporary unencrypted import file output stream.
                            temporaryUnencryptedImportFileOutputStream.write(decryptedBytes, 0, numberOfBytesRead);
                        }


                        // Flush the temporary unencrypted import file output stream.
                        temporaryUnencryptedImportFileOutputStream.flush();

                        // Close the streams.
                        temporaryUnencryptedImportFileOutputStream.close();
                        cipherInputStream.close();
                        inputStream.close();

                        // Wipe the encryption data from memory.
                        //noinspection UnusedAssignment
                        encryptionPasswordString = "";
                        Arrays.fill(saltByteArray, (byte) 0);
                        Arrays.fill(initializationVector, (byte) 0);
                        Arrays.fill(encryptionPasswordByteArray, (byte) 0);
                        Arrays.fill(encryptionPasswordWithSaltByteArray, (byte) 0);
                        Arrays.fill(hashedEncryptionPasswordWithSaltByteArray, (byte) 0);
                        Arrays.fill(truncatedHashedEncryptionPasswordWithSaltByteArray, (byte) 0);
                        Arrays.fill(decryptedBytes, (byte) 0);

                        // Create a temporary unencrypted import file input stream.
                        FileInputStream temporaryUnencryptedImportFileInputStream = new FileInputStream(temporaryUnencryptedImportFile);

                        // Import the temporary unencrypted import file.
                        importStatus = importExportDatabaseHelper.importUnencrypted(temporaryUnencryptedImportFileInputStream, this);

                        // Close the temporary unencrypted import file input stream.
                        temporaryUnencryptedImportFileInputStream.close();

                        // Delete the temporary unencrypted import file.
                        //noinspection ResultOfMethodCallIgnored
                        temporaryUnencryptedImportFile.delete();

                        // Restart Privacy Browser if successful.
                        if (importStatus.equals(ImportExportDatabaseHelper.IMPORT_SUCCESSFUL)) {
                            restartPrivacyBrowser();
                        }
                    } catch (Exception exception) {
                        // Update the import status.
                        importStatus = exception.toString();
                    }
                    break;

                case OPENPGP_ENCRYPTION:
                    try {
                        // Set the temporary PGP encrypted import file.
                        temporaryPgpEncryptedImportFile = File.createTempFile("temporary_pgp_encrypted_import_file", null, getApplicationContext().getCacheDir());

                        // Create a temporary PGP encrypted import file output stream.
                        FileOutputStream temporaryPgpEncryptedImportFileOutputStream = new FileOutputStream(temporaryPgpEncryptedImportFile);

                        // Get an input stream for the file name.
                        InputStream inputStream = getContentResolver().openInputStream(Uri.parse(fileNameString));

                        // Create a transfer byte array.
                        byte[] transferByteArray = new byte[1024];

                        // Create an integer to track the number of bytes read.
                        int bytesRead;

                        // Copy the input stream to the temporary PGP encrypted import file.
                        while ((bytesRead = inputStream.read(transferByteArray)) > 0) {
                            temporaryPgpEncryptedImportFileOutputStream.write(transferByteArray, 0, bytesRead);
                        }

                        // Flush the temporary PGP encrypted import file output stream.
                        temporaryPgpEncryptedImportFileOutputStream.flush();

                        // Close the streams.
                        inputStream.close();
                        temporaryPgpEncryptedImportFileOutputStream.close();

                        // Create an decryption intent for OpenKeychain.
                        Intent openKeychainDecryptIntent = new Intent("org.sufficientlysecure.keychain.action.DECRYPT_DATA");

                        // Include the URI to be decrypted.
                        openKeychainDecryptIntent.setData(FileProvider.getUriForFile(this, getString(R.string.file_provider), temporaryPgpEncryptedImportFile));

                        // Allow OpenKeychain to read the file URI.
                        openKeychainDecryptIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                        // Send the intent to the OpenKeychain package.
                        openKeychainDecryptIntent.setPackage("org.sufficientlysecure.keychain");

                        // Make it so.
                        startActivityForResult(openKeychainDecryptIntent, OPENPGP_IMPORT_RESULT_CODE);

                        // Update the import status.
                        importStatus = ImportExportDatabaseHelper.IMPORT_SUCCESSFUL;
                    } catch (Exception exception) {
                        // Update the import status.
                        importStatus = exception.toString();
                    }
                    break;
            }

            // Respond to the import status.
            if (!importStatus.equals(ImportExportDatabaseHelper.IMPORT_SUCCESSFUL)) {
                // Display a snack bar with the import error.
                Snackbar.make(fileNameEditText, getString(R.string.import_failed) + "  " + importStatus, Snackbar.LENGTH_INDEFINITE).show();
            }
        } else {  // Export is selected.
            // Export according to the encryption type.
            switch (encryptionSpinner.getSelectedItemPosition()) {
                case NO_ENCRYPTION:
                    // Get the file name string.
                    String noEncryptionFileNameString = fileNameEditText.getText().toString();

                    try {
                        // Get the export file output stream.
                        OutputStream exportFileOutputStream = getContentResolver().openOutputStream(Uri.parse(noEncryptionFileNameString));

                        // Export the unencrypted file.
                        String noEncryptionExportStatus = importExportDatabaseHelper.exportUnencrypted(exportFileOutputStream, this);

                        // Display an export disposition snackbar.
                        if (noEncryptionExportStatus.equals(ImportExportDatabaseHelper.EXPORT_SUCCESSFUL)) {
                            Snackbar.make(fileNameEditText, getString(R.string.export_successful), Snackbar.LENGTH_SHORT).show();
                        } else {
                            Snackbar.make(fileNameEditText, getString(R.string.export_failed) + "  " + noEncryptionExportStatus, Snackbar.LENGTH_INDEFINITE).show();
                        }
                    } catch (FileNotFoundException fileNotFoundException) {
                        // Display a snackbar with the exception.
                        Snackbar.make(fileNameEditText, getString(R.string.export_failed) + "  " + fileNotFoundException, Snackbar.LENGTH_INDEFINITE).show();
                    }
                    break;

                case PASSWORD_ENCRYPTION:
                    try {
                        // Create a temporary unencrypted export file.
                        File temporaryUnencryptedExportFile = File.createTempFile("temporary_unencrypted_export_file", null, getApplicationContext().getCacheDir());

                        // Create a temporary unencrypted export output stream.
                        FileOutputStream temporaryUnencryptedExportOutputStream = new FileOutputStream(temporaryUnencryptedExportFile);

                        // Populate the temporary unencrypted export.
                        String passwordEncryptionExportStatus = importExportDatabaseHelper.exportUnencrypted(temporaryUnencryptedExportOutputStream, this);

                        // Close the temporary unencrypted export output stream.
                        temporaryUnencryptedExportOutputStream.close();

                        // Create an unencrypted export file input stream.
                        FileInputStream unencryptedExportFileInputStream = new FileInputStream(temporaryUnencryptedExportFile);

                        // Get the encryption password.
                        String encryptionPasswordString = encryptionPasswordEditText.getText().toString();

                        // Initialize a secure random number generator.
                        SecureRandom secureRandom = new SecureRandom();

                        // Get a 256 bit (32 byte) random salt.
                        byte[] saltByteArray = new byte[32];
                        secureRandom.nextBytes(saltByteArray);

                        // Convert the encryption password to a byte array.
                        byte[] encryptionPasswordByteArray = encryptionPasswordString.getBytes(StandardCharsets.UTF_8);

                        // Append the salt to the encryption password byte array.  This protects against rainbow table attacks.
                        byte[] encryptionPasswordWithSaltByteArray = new byte[encryptionPasswordByteArray.length + saltByteArray.length];
                        System.arraycopy(encryptionPasswordByteArray, 0, encryptionPasswordWithSaltByteArray, 0, encryptionPasswordByteArray.length);
                        System.arraycopy(saltByteArray, 0, encryptionPasswordWithSaltByteArray, encryptionPasswordByteArray.length, saltByteArray.length);

                        // Get a SHA-512 message digest.
                        MessageDigest messageDigest = MessageDigest.getInstance("SHA-512");

                        // Hash the salted encryption password.  Otherwise, any characters after the 32nd character in the password are ignored.
                        byte[] hashedEncryptionPasswordWithSaltByteArray = messageDigest.digest(encryptionPasswordWithSaltByteArray);

                        // Truncate the encryption password byte array to 256 bits (32 bytes).
                        byte[] truncatedHashedEncryptionPasswordWithSaltByteArray = Arrays.copyOf(hashedEncryptionPasswordWithSaltByteArray, 32);

                        // Create an AES secret key from the encryption password byte array.
                        SecretKeySpec secretKey = new SecretKeySpec(truncatedHashedEncryptionPasswordWithSaltByteArray, "AES");

                        // Generate a random 12 byte initialization vector.  According to NIST, a 12 byte initialization vector is more secure than a 16 byte one.
                        byte[] initializationVector = new byte[12];
                        secureRandom.nextBytes(initializationVector);

                        // Get a Advanced Encryption Standard, Galois/Counter Mode, No Padding cipher instance. Galois/Counter mode protects against modification of the ciphertext.  It doesn't use padding.
                        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");

                        // Set the GCM tag length to be 128 bits (the maximum) and apply the initialization vector.
                        GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(128, initializationVector);

                        // Initialize the cipher.
                        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmParameterSpec);

                        // Get the file name string.
                        String passwordEncryptionFileNameString = fileNameEditText.getText().toString();

                        // Get the export file output stream.
                        OutputStream exportFileOutputStream = getContentResolver().openOutputStream(Uri.parse(passwordEncryptionFileNameString));

                        // Add the salt and the initialization vector to the export file output stream.
                        exportFileOutputStream.write(saltByteArray);
                        exportFileOutputStream.write(initializationVector);

                        // Create a cipher output stream.
                        CipherOutputStream cipherOutputStream = new CipherOutputStream(exportFileOutputStream, cipher);

                        // Initialize variables to store data as it is moved from the unencrypted export file input stream to the cipher output stream.  Move 128 bits (16 bytes) at a time.
                        int numberOfBytesRead;
                        byte[] encryptedBytes = new byte[16];

                        // Read up to 128 bits (16 bytes) of data from the unencrypted export file stream.  `-1` will be returned when the end of the file is reached.
                        while ((numberOfBytesRead = unencryptedExportFileInputStream.read(encryptedBytes)) != -1) {
                            // Write the data to the cipher output stream.
                            cipherOutputStream.write(encryptedBytes, 0, numberOfBytesRead);
                        }

                        // Close the streams.
                        cipherOutputStream.flush();
                        cipherOutputStream.close();
                        exportFileOutputStream.close();
                        unencryptedExportFileInputStream.close();

                        // Wipe the encryption data from memory.
                        //noinspection UnusedAssignment
                        encryptionPasswordString = "";
                        Arrays.fill(saltByteArray, (byte) 0);
                        Arrays.fill(encryptionPasswordByteArray, (byte) 0);
                        Arrays.fill(encryptionPasswordWithSaltByteArray, (byte) 0);
                        Arrays.fill(hashedEncryptionPasswordWithSaltByteArray, (byte) 0);
                        Arrays.fill(truncatedHashedEncryptionPasswordWithSaltByteArray, (byte) 0);
                        Arrays.fill(initializationVector, (byte) 0);
                        Arrays.fill(encryptedBytes, (byte) 0);

                        // Delete the temporary unencrypted export file.
                        //noinspection ResultOfMethodCallIgnored
                        temporaryUnencryptedExportFile.delete();

                        // Display an export disposition snackbar.
                        if (passwordEncryptionExportStatus.equals(ImportExportDatabaseHelper.EXPORT_SUCCESSFUL)) {
                            Snackbar.make(fileNameEditText, getString(R.string.export_successful), Snackbar.LENGTH_SHORT).show();
                        } else {
                            Snackbar.make(fileNameEditText, getString(R.string.export_failed) + "  " + passwordEncryptionExportStatus, Snackbar.LENGTH_INDEFINITE).show();
                        }
                    } catch (Exception exception) {
                        // Display a snackbar with the exception.
                        Snackbar.make(fileNameEditText, getString(R.string.export_failed) + "  " + exception, Snackbar.LENGTH_INDEFINITE).show();
                    }
                    break;

                case OPENPGP_ENCRYPTION:
                    try {
                        // Set the temporary pre-encrypted export file.
                        temporaryPreEncryptedExportFile = new File(getApplicationContext().getCacheDir() + "/" + getString(R.string.settings) + " " + BuildConfig.VERSION_NAME + ".pbs");

                        // Delete the temporary pre-encrypted export file if it already exists.
                        if (temporaryPreEncryptedExportFile.exists()) {
                            //noinspection ResultOfMethodCallIgnored
                            temporaryPreEncryptedExportFile.delete();
                        }

                        // Create a temporary pre-encrypted export output stream.
                        FileOutputStream temporaryPreEncryptedExportOutputStream = new FileOutputStream(temporaryPreEncryptedExportFile);

                        // Populate the temporary pre-encrypted export file.
                        String openpgpEncryptionExportStatus = importExportDatabaseHelper.exportUnencrypted(temporaryPreEncryptedExportOutputStream, this);

                        // Flush the temporary pre-encryption export output stream.
                        temporaryPreEncryptedExportOutputStream.flush();

                        // Close the temporary pre-encryption export output stream.
                        temporaryPreEncryptedExportOutputStream.close();

                        // Display an export error snackbar if the temporary pre-encrypted export failed.
                        if (!openpgpEncryptionExportStatus.equals(ImportExportDatabaseHelper.EXPORT_SUCCESSFUL)) {
                            Snackbar.make(fileNameEditText, getString(R.string.export_failed) + "  " + openpgpEncryptionExportStatus, Snackbar.LENGTH_INDEFINITE).show();
                        }

                        // Create an encryption intent for OpenKeychain.
                        Intent openKeychainEncryptIntent = new Intent("org.sufficientlysecure.keychain.action.ENCRYPT_DATA");

                        // Include the temporary unencrypted export file URI.
                        openKeychainEncryptIntent.setData(FileProvider.getUriForFile(this, getString(R.string.file_provider), temporaryPreEncryptedExportFile));

                        // Allow OpenKeychain to read the file URI.
                        openKeychainEncryptIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                        // Send the intent to the OpenKeychain package.
                        openKeychainEncryptIntent.setPackage("org.sufficientlysecure.keychain");

                        // Make it so.
                        startActivityForResult(openKeychainEncryptIntent, OPENPGP_EXPORT_RESULT_CODE);
                    } catch (Exception exception) {
                        // Display a snackbar with the exception.
                        Snackbar.make(fileNameEditText, getString(R.string.export_failed) + "  " + exception, Snackbar.LENGTH_INDEFINITE).show();
                    }
                    break;
            }
        }
    }

    private void restartPrivacyBrowser() {
        // Create an intent to restart Privacy Browser.
        Intent restartIntent = getParentActivityIntent();

        // Assert that the intent is not null to remove the lint error below.
        assert restartIntent != null;

        // `Intent.FLAG_ACTIVITY_CLEAR_TASK` removes all activities from the stack.  It requires `Intent.FLAG_ACTIVITY_NEW_TASK`.
        restartIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        // Create a restart handler.
        Handler restartHandler = new Handler();

        // Create a restart runnable.
        Runnable restartRunnable =  () -> {
            // Restart Privacy Browser.
            startActivity(restartIntent);

            // Kill this instance of Privacy Browser.  Otherwise, the app exhibits sporadic behavior after the restart.
            System.exit(0);
        };

        // Restart Privacy Browser after 150 milliseconds to allow enough time for the preferences to be saved.
        restartHandler.postDelayed(restartRunnable, 150);
    }
}