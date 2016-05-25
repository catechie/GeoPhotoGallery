package com.boobastudio.geophotogallery;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by donna on 5/22/16.
 */
public class GalleryAdapter extends BaseAdapter {
        ArrayList list;
        int layout;
        Context context;
    public GalleryAdapter(ArrayList<PhotoInfo> itemList, int itemLayout, Context c) {

        list = itemList;
        layout = itemLayout;
        context = c;
    }

    @Override
    public int getCount() {
        return (list==null)? 0:list.size();
    }

    @Override
    public Object getItem(int position) {
        return list.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        //MainActivity.ViewHolder thumb;
        final View v;
        if (convertView == null) {
            v = LayoutInflater.from(context).inflate(R.layout.photo_item, null);
            new AsyncTask<PhotoInfo, Void, Bitmap>(){
                @Override
                protected Bitmap doInBackground(PhotoInfo... params) {
                    byte [] bitMapBytes = RestfulCallFetchResult.queryJsonResult(params[0].getUrl_S());
                    return BitmapFactory.decodeByteArray(bitMapBytes, 0, bitMapBytes.length);
                }

                @Override
                protected void onPostExecute(Bitmap bitmap) {
                    super.onPostExecute(bitmap);
                    ImageView item = ((ImageView)v.findViewById(R.id.imageViewThumb));
                    item.setImageBitmap(bitmap);
                    item.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent i = new Intent();
                            String url = ((PhotoInfo)list.get(position)).getUrl_L();
                            i.putExtra("url", url );
                            i.setClass(context, ItemViewActivity.class);
                            context.startActivity(i);
                            Log.d("GalleryAdapter", "url_l : "+url);
                        }
                    });
                    ((TextView) v.findViewById(R.id.url_large)).setText(((PhotoInfo) list.get(position)).getUrl_L());

                }
            }.execute(((PhotoInfo)list.get(position)));
            return v;
        }
        else return v = convertView;
    }
}
