package com.imagesearcher;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;

import butterknife.BindView;
import butterknife.ButterKnife;

public class DisplayActivity extends AppCompatActivity {

    @BindView(R.id.chosen_image_IV) ImageView chosenImageIV;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display);

        ButterKnife.bind(this);

        Intent intent = getIntent();
        if (getIntent().hasExtra(MainActivity.CHOSEN_IMAGE_LINK)) {
            String imageLink = intent.getStringExtra(MainActivity.CHOSEN_IMAGE_LINK);
            Picasso.with(getApplicationContext())
                    .load(imageLink)
                    .error(R.drawable.ic_warning_yellow_24dp)
                    .into(chosenImageIV);
        }
    }
}
