package com.example.card_text;

import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ClickableSpan;
import android.text.style.StyleSpan;
import android.view.View;
import android.widget.TextView;
import android.graphics.Typeface;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import android.text.method.LinkMovementMethod;

public class MainActivity extends AppCompatActivity {

    private boolean isExpanded = false;
    private TextView textView;
    private String fullText = "This is the first line of the text. This is the second line of the text. This is the third line of the text which should be hidden initially.";
    private String collapsedText = "This is the first line of the text. This is the second line of the text.";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView = findViewById(R.id.textView);
        setCollapsedText();
    }

    private void setCollapsedText() {
        SpannableString spannableString = new SpannableString(collapsedText + " Read More");
        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                setExpandedText();
            }
        };
        spannableString.setSpan(clickableSpan, collapsedText.length() + 1, spannableString.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannableString.setSpan(new StyleSpan(Typeface.BOLD), collapsedText.length() + 1, spannableString.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannableString.setSpan(new android.text.style.ForegroundColorSpan(ContextCompat.getColor(this, android.R.color.holo_blue_dark)), collapsedText.length() + 1, spannableString.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        textView.setText(spannableString);
        textView.setMovementMethod(LinkMovementMethod.getInstance());
        textView.setMaxLines(2);
    }

    private void setExpandedText() {
        SpannableString spannableString = new SpannableString(fullText + " Read Less");
        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                setCollapsedText();
            }
        };
        spannableString.setSpan(clickableSpan, fullText.length() + 1, spannableString.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannableString.setSpan(new StyleSpan(Typeface.BOLD), fullText.length() + 1, spannableString.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannableString.setSpan(new android.text.style.ForegroundColorSpan(ContextCompat.getColor(this, android.R.color.holo_blue_dark)), fullText.length() + 1, spannableString.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        textView.setText(spannableString);
        textView.setMovementMethod(LinkMovementMethod.getInstance());
        textView.setMaxLines(Integer.MAX_VALUE);
    }
}
