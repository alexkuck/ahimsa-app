package io.ahimsa.ahimsa_app.ui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import io.ahimsa.ahimsa_app.Configuration;
import io.ahimsa.ahimsa_app.Constants;
import io.ahimsa.ahimsa_app.R;
import io.ahimsa.ahimsa_app.core.AhimsaService;
import io.ahimsa.ahimsa_app.fund.FundService;
import io.ahimsa.ahimsa_app.util.Qr;


public class OverviewFragment extends Fragment {

    public OverviewFragment() {
        // Empty constructor
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View v = inflater.inflate(R.layout.fragment_overview2, container, false);
        updateView(v, getArguments());

        Button importBlockButton = (Button) v.findViewById(R.id.import_block_button);
        // todo | window leaked error on screen rotation
        importBlockButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View V) {
                LayoutInflater layout_inflater = getActivity().getLayoutInflater();
                final View inflater = layout_inflater.inflate(R.layout.dialog_import, null);
                AlertDialog.Builder builer = new AlertDialog.Builder(getActivity());

                builer.setTitle("Import a Block");
                builer.setMessage("This little popup will discover all relevant funding transactions within a block.");
                builer.setView(inflater);

                final EditText heightEditText = (EditText) inflater.findViewById(R.id.height_edit_text);

                builer.setPositiveButton("Magic!", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                try {
                                    Long height = new Long( heightEditText.getText().toString() );
                                    AhimsaService.startImportBlock(getActivity(), height);
                                } catch(Exception e) {
                                    Toast.makeText(getActivity(), R.string.invalid_import_height, Toast.LENGTH_LONG).show();
                                }
                            }
                        });

                builer.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // do nothing
                            }
                        });

                final AlertDialog dialog = builer.create();

                heightEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                    @Override
                    public void onFocusChange(View v, boolean hasFocus) {
                        if (hasFocus) {
                            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                        }
                    }
                });

                dialog.show();

            }
        });

        Button requestCoinButton = (Button) v.findViewById(R.id.request_coin_button);
        requestCoinButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View V) {
                FundService.startRequestFundingTxUseConfig(getActivity());
            }
        });

        return v;
    }

    public void updateView(Bundle args) {
        updateView(getView(), args);
    }

    protected void updateView(View v, Bundle args) {
        if(v != null){
            final TextView address_eight_value = (TextView) v.findViewById(R.id.address_value);
            final TextView checksum_value = (TextView) v.findViewById(R.id.checksum_value);
            String address = args.getString(Constants.EXTRA_STRING_ADDRESS);
            if(address.length() > 8) {
                address_eight_value.setText(address.substring(1,9));
                checksum_value.setText(address.substring(address.length()-4));
            }

            final TextView available_balance = (TextView) v.findViewById(R.id.available_satoshi_value);
            String avail_bal = new Long(args.getLong(Constants.EXTRA_LONG_AVAILABLE_BAL)).toString();
            avail_bal = avail_bal.replaceAll(Constants.COMMA_REGEX_1, Constants.COMMA_REGEX_2);
            available_balance.setText(avail_bal);

            final TextView txouts_value = (TextView) v.findViewById(R.id.available_txout_value);
            Integer avail_txouts = args.getInt(Constants.EXTRA_INT_AVAILABLE_TXOUTS);
            txouts_value.setText(avail_txouts.toString());



            final TextView character_estimate = (TextView) v.findViewById(R.id.character_estimate_value);

            final TextView bulletin_estimate = (TextView) v.findViewById(R.id.bulletin_estimate_value);


            final TextView confirmed_count = (TextView) v.findViewById(R.id.confirmed_count_value);

            final TextView unconfirmed_count = (TextView) v.findViewById(R.id.unconfirmed_count_value);



            final Bitmap qr_address = Qr.bitmap(args.getString(Constants.EXTRA_STRING_ADDRESS), 256);
            ImageView qr_view = (ImageView) v.findViewById(R.id.qr_value);
            qr_view.setImageBitmap(qr_address);

            final TextView full_address_value = (TextView) v.findViewById(R.id.full_address_value);
            full_address_value.setText(address);




//            final TextView unconf_tx_value = (TextView) v.findViewById(R.id.unconfirmed_tx_value);
//            Integer unconf_tx = new Integer(args.getInt(Constants.EXTRA_INT_UNCONF));
//            unconf_tx_value.setText(unconf_tx.toString() + " Unconfirmed Transactions");
//
//            switch (unconf_tx){
//                case 0: unconf_tx_value.setTextColor(Color.BLACK);
//                        break;
//                case 1: unconf_tx_value.setText(unconf_tx.toString() + " Unconfirmed Transaction");
//            }



        }
    }

    public static OverviewFragment newInstance(Bundle args)
    {
        OverviewFragment frag = new OverviewFragment();
        frag.setArguments(args);
        return frag;
    }

}
