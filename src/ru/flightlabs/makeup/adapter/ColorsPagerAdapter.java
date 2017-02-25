package ru.flightlabs.makeup.adapter;

import android.content.Context;
import android.graphics.Color;
import android.support.v4.view.PagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import ru.flightlabs.makeup.R;

/**
 * Created by sov on 19.11.2016.
 */

public class ColorsPagerAdapter extends PagerAdapter {

    AdaptersNotifier fdAct;
    Context mContext;
    int[] colors;
    LayoutInflater mLayoutInflater;

    public ColorsPagerAdapter(AdaptersNotifier context, int[] colors) {
        mContext = (Context) context;
        mLayoutInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.fdAct = context;
        this.colors = colors;
    }

    @Override
    public int getCount() {
        return colors.length;
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == ((FrameLayout) object);
    }

    @Override
    public Object instantiateItem(ViewGroup container, final int position) {
        View itemView = mLayoutInflater.inflate(R.layout.item_color, container, false);

        ImageView imageView = (ImageView) itemView.findViewById(R.id.item_image);
        imageView.setColorFilter(colors[position]);
        container.addView(itemView);
        itemView.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                fdAct.changeColor(colors[position], position);

            }
        });
        return itemView;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView((FrameLayout) object);
    }

    @Override
    public float getPageWidth(int position) {
        return 1f / 4;
    }
}