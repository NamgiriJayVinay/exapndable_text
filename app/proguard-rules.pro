# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile




ppp

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.viewpager.widget.ViewPager;

public class WrapContentViewPager extends ViewPager {
    public WrapContentViewPager(Context context) {
        super(context);
    }

    public WrapContentViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int height = 0;
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            child.measure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
            int h = child.getMeasuredHeight();
            if (h > height) height = h; // Use the tallest child
        }
        heightMeasureSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    mm
99


public class ViewPager2HeightAdjuster {

    public static void adjustViewPagerHeight(ViewPager2 viewPager2) {
        View child = ((RecyclerView) viewPager2.getChildAt(0)).getLayoutManager().findViewByPosition(0);

        if (child != null) {
            child.measure(
                    View.MeasureSpec.makeMeasureSpec(viewPager2.getWidth(), View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            );

            int height = child.getMeasuredHeight();
            if (height > 0) {
                ViewGroup.LayoutParams layoutParams = viewPager2.getLayoutParams();
                layoutParams.height = height;
                viewPager2.setLayoutParams(layoutParams);
            }
        }
    }
} 


package com.example.dynamicviewpager2;

import android.os.Bundle;
import android.widget.ScrollView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ScrollView scrollView;
    private ViewPager2 viewPager2;
    private CardPagerAdapter2 cardPagerAdapter;
    private List<String> cardList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        scrollView = findViewById(R.id.scrollView);
        viewPager2 = findViewById(R.id.viewPager);

        // Initialize card list and adapter
        cardList = new ArrayList<>();
        cardPagerAdapter = new CardPagerAdapter2(cardList);
        viewPager2.setAdapter(cardPagerAdapter);

        // Adjust ViewPager2 height initially
        viewPager2.post(() -> ViewPager2HeightAdjuster.adjustViewPagerHeight(viewPager2));

        // Add a new card when the button is clicked
        findViewById(R.id.addCardButton).setOnClickListener(v -> addNewCard("New Card"));
    }

    private void addNewCard(String cardText) {
        // Add a new card to the list
        cardList.add(cardText);

        // Notify the adapter about the new data
        cardPagerAdapter.notifyDataSetChanged();

        // Adjust ViewPager2 height after content change
        viewPager2.post(() -> ViewPager2HeightAdjuster.adjustViewPagerHeight(viewPager2));

        // Ensure the ScrollView scrolls to the bottom
        scrollView.post(() -> scrollView.fullScroll(ScrollView.FOCUS_DOWN));
    }
}




package com.example.dynamicviewpager2;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class CardPagerAdapter2 extends RecyclerView.Adapter<CardPagerAdapter2.ViewHolder> {

    private List<String> cardList;

    public CardPagerAdapter2(List<String> cardList) {
        this.cardList = cardList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.cardText.setText(cardList.get(position));
    }

    @Override
    public int getItemCount() {
        return cardList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView cardText;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardText = itemView.findViewById(R.id.cardText);
        }
    }
}




