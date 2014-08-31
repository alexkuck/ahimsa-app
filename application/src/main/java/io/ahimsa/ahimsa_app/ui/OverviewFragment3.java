package io.ahimsa.ahimsa_app.ui;

import android.app.Fragment;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import io.ahimsa.ahimsa_app.Constants;
import io.ahimsa.ahimsa_app.R;
import io.ahimsa.ahimsa_app.util.Qr;

public class OverviewFragment3 extends Fragment {

    public OverviewFragment3()
    {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View v = inflater.inflate(R.layout.fragment_overview3, container, false);
        updateView(v, getArguments());

        return v;
    }

    public void updateView(Bundle args)
    {
        updateView(getView(), args);
    }

    protected void updateView(View v, Bundle args)
    {
        if (v != null)
        {
            TextView address_value = (TextView) v.findViewById(R.id.address_value);
            String address = args.getString(Constants.EXTRA_STRING_ADDRESS);
            StringBuilder two_line_address = new StringBuilder(address);
            two_line_address.insert(two_line_address.length() / 2, "\n");
//            address_value.setText(two_line_address);


            final SpannableStringBuilder sb = new SpannableStringBuilder(two_line_address);
            final ForegroundColorSpan fcs = new ForegroundColorSpan(Color.rgb(0, 176, 176));

            // Span to set text color to some RGB value
//            final StyleSpan bss = new StyleSpan(android.graphics.Typeface.BOLD);

            // Set the text color for first 4 characters
            sb.setSpan(fcs, 1, 9, Spannable.SPAN_INCLUSIVE_INCLUSIVE);

            // Span to make text bold
//            sb.setSpan(bss, 0, 4, Spannable.SPAN_INCLUSIVE_INCLUSIVE);

            // make them also bold
            address_value.setText(sb);




            final Bitmap qr_address = Qr.bitmap(address, 135);
            ImageView qr_view = (ImageView) v.findViewById(R.id.qr_value);
            qr_view.setImageBitmap(qr_address);
        }
    }

    public static OverviewFragment3 newInstance(Bundle args)
    {
        OverviewFragment3 frag = new OverviewFragment3();
        frag.setArguments(args);
        return frag;
    }

}
