package com.qralarm.app;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

public class QRSetupActivity extends AppCompatActivity {

    public static final int REQUEST_CODE = 7001;
    public static final String EXTRA_CURRENT_VALUE = "extra_current_value";
    public static final String EXTRA_CURRENT_LABEL = "extra_current_label";
    public static final String RESULT_QR_VALUE = "result_qr_value";
    public static final String RESULT_QR_LABEL = "result_qr_label";

    private EditText etCode, etLabel;
    private TextView tvCurrent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_setup);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.setup_qr_title);
        }

        etCode = findViewById(R.id.et_qr_code);
        etLabel = findViewById(R.id.et_qr_label);
        tvCurrent = findViewById(R.id.tv_current_qr);

        String currentValue = getIntent().getStringExtra(EXTRA_CURRENT_VALUE);
        String currentLabel = getIntent().getStringExtra(EXTRA_CURRENT_LABEL);
        if (currentValue != null && !currentValue.isEmpty()) {
            etCode.setText(currentValue);
            etLabel.setText(currentLabel != null ? currentLabel : "");
            tvCurrent.setText(getString(R.string.current_qr_value, currentValue));
        }

        findViewById(R.id.btn_scan_to_set).setOnClickListener(v -> startScan());
        findViewById(R.id.btn_save_qr).setOnClickListener(v -> saveAndReturn());
        findViewById(R.id.btn_clear_qr).setOnClickListener(v -> {
            etCode.setText("");
            etLabel.setText("");
            tvCurrent.setText(R.string.no_qr_configured_setup);
        });
    }

    private void startScan() {
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setDesiredBarcodeFormats(IntentIntegrator.ALL_CODE_TYPES);
        integrator.setPrompt(getString(R.string.scan_prompt_setup));
        integrator.setBeepEnabled(true);
        integrator.setOrientationLocked(true);
        integrator.initiateScan();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() != null) {
                etCode.setText(result.getContents());
                tvCurrent.setText(getString(R.string.scanned_value, result.getContents()));
                Toast.makeText(this, R.string.scan_success, Toast.LENGTH_SHORT).show();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void saveAndReturn() {
        String code = etCode.getText().toString().trim();
        String label = etLabel.getText().toString().trim();

        if (TextUtils.isEmpty(code)) {
            Toast.makeText(this, R.string.qr_code_empty_warning, Toast.LENGTH_SHORT).show();
            // Allow saving empty to clear the QR requirement
        }

        Intent result = new Intent();
        result.putExtra(RESULT_QR_VALUE, code);
        result.putExtra(RESULT_QR_LABEL, label);
        setResult(RESULT_OK, result);
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
