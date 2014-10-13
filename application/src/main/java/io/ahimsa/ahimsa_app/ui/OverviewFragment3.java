package io.ahimsa.ahimsa_app.ui;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import io.ahimsa.ahimsa_app.Constants;
import io.ahimsa.ahimsa_app.R;
import io.ahimsa.ahimsa_app.core.AhimsaService;
import io.ahimsa.ahimsa_app.core.Utils;
import io.ahimsa.ahimsa_app.util.Qr;

public class OverviewFragment3 extends Fragment
{
    private Bitmap qrCodeBitmap;

    public OverviewFragment3()
    {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View v = inflater.inflate(R.layout.fragment_overview3, container, false);

        Button importBlockButton = (Button) v.findViewById(R.id.search_for_funds_button);
        // todo | window leaked error on screen rotation
        importBlockButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View V)
            {
                LayoutInflater layout_inflater = getActivity().getLayoutInflater();
                final View inflater = layout_inflater.inflate(R.layout.dialog_import, null);
                AlertDialog.Builder builer = new AlertDialog.Builder(getActivity());

                builer.setTitle("Import a Block");
                builer.setMessage("Discover all relevant funding transactions within a block.");
                builer.setView(inflater);

                final EditText heightEditText = (EditText) inflater.findViewById(R.id.height_edit_text);

                builer.setPositiveButton("Magic!", new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int which)
                    {
                        try
                        {
                            Long height = new Long( heightEditText.getText().toString() );
                            AhimsaService.startImportBlock(getActivity(), height);
                            Toast.makeText(getActivity(), "Import block request.\nheight: " + height, Toast.LENGTH_LONG).show();
                        }
                        catch(Exception e)
                        {
                            Toast.makeText(getActivity(), R.string.invalid_import_height, Toast.LENGTH_LONG).show();
                        }
                    }
                });

                builer.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int which) {
                        // do nothing
                    }
                });

                final AlertDialog dialog = builer.create();

                heightEditText.setOnFocusChangeListener(new View.OnFocusChangeListener()
                {
                    @Override
                    public void onFocusChange(View v, boolean hasFocus)
                    {
                        if(hasFocus)
                        {
                            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                        }
                    }
                });

                dialog.show();

            }
        });

        final TextView address_value = (TextView) v.findViewById(R.id.address_value);
        address_value.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(getActivity().CLIPBOARD_SERVICE);
                String address = getArguments().getString(Constants.EXTRA_STRING_ADDRESS);
                ClipData clip = ClipData.newPlainText("ahimsa-app address", address);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(getActivity(), "Copied device address to clipboard", Toast.LENGTH_SHORT).show();
            }
        });


        // todo: wish i didn't have to make this class specific.. possibly use a list fragment?
        ListView outpoint_list = (ListView) v.findViewById(R.id.outpoint_list);
        AhimsaActivity activity = (AhimsaActivity) getActivity();
        OutPointCursorAdapter outpoint_adapter = new OutPointCursorAdapter(getActivity(), R.layout.listview_item_outpoint, activity.getOutPointCursor(), -1);
        outpoint_list.setAdapter( outpoint_adapter );

        updateView(inflater, v, getArguments());
        return v;
    }

    public void updateView(LayoutInflater inflater, Bundle args)
    {
        updateView(inflater, getView(), args);
    }

    protected void updateView(LayoutInflater inflater, View v, Bundle args)
    {
        if (v != null)
        {
            TextView address_value = (TextView) v.findViewById(R.id.address_value);
            String address = args.getString(Constants.EXTRA_STRING_ADDRESS);
            StringBuilder two_line_address = new StringBuilder(address);
            two_line_address.insert(12, "\n");
            two_line_address.insert(25, "\n");

            final SpannableStringBuilder sb = new SpannableStringBuilder(two_line_address);
            final ForegroundColorSpan fcs = new ForegroundColorSpan(Color.rgb(0, 176, 176));
            sb.setSpan(fcs, 1, 9, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            address_value.setText(sb);

            int size = (int) (256 * getResources().getDisplayMetrics().density);
            qrCodeBitmap = Qr.bitmap(address, size);
            ImageView qr_view = (ImageView) v.findViewById(R.id.qr_value);
            qr_view.setImageBitmap(qrCodeBitmap);
            qr_view.setOnClickListener( new View.OnClickListener()
            {
                @Override
                public void onClick(View view)
                {
                    handleShowQRCode();
                }
            });

            final TextView wallet_value = (TextView) v.findViewById(R.id.wallet_value);
            Long avail_bal = new Long(args.getLong(Constants.EXTRA_LONG_AVAILABLE_BAL));
            wallet_value.setText(Utils.commarizer(avail_bal.toString()));

            final TextView character_value = (TextView) v.findViewById(R.id.character_value);
            character_value.setText( "~" + Utils.commarizer(Utils.characterEstimator(avail_bal).toString()) );

            ListView outpoint_list = (ListView) v.findViewById(R.id.outpoint_list);
//            View header = inflater.inflate(R.layout.listview_item_outpoint_header, null);
//            outpoint_list.addHeaderView(header);
            OutPointCursorAdapter outpoint_cursor = (OutPointCursorAdapter) outpoint_list.getAdapter();
            AhimsaActivity activity = (AhimsaActivity) getActivity();
            outpoint_cursor.swapCursor( activity.getOutPointCursor() );

        }
    }

    private void handleShowQRCode()
    {
        BitmapFragment.show(getFragmentManager(), qrCodeBitmap);
    }

    public static OverviewFragment3 newInstance(Bundle args)
    {
        OverviewFragment3 frag = new OverviewFragment3();
        frag.setArguments(args);
        return frag;
    }

}
