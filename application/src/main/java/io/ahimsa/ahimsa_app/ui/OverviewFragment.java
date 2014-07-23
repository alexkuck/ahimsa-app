package io.ahimsa.ahimsa_app.ui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import io.ahimsa.ahimsa_app.Configuration;
import io.ahimsa.ahimsa_app.Constants;
import io.ahimsa.ahimsa_app.R;
import io.ahimsa.ahimsa_app.core.AhimsaService;
import io.ahimsa.ahimsa_app.fund.FundService;


public class OverviewFragment extends Fragment {

    public OverviewFragment() {
        // Empty constructor
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View v = inflater.inflate(R.layout.fragment_overview, container, false);
        updateView(v, getArguments());

        Button importBlockButton = (Button) v.findViewById(R.id.import_block_button);
        // todo | window leaked error on screen rotation
        importBlockButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View V) {
                LayoutInflater layout_inflater = getActivity().getLayoutInflater();
                final View inflater = layout_inflater.inflate(R.layout.dialog_import, null);
                AlertDialog.Builder builer = new AlertDialog.Builder(getActivity());

                builer.setTitle("Import Block");
                builer.setMessage("Discover and import relevant funding transactions.");
                builer.setView(inflater);

                final EditText heightEditText = (EditText) inflater.findViewById(R.id.height_edit_text);

                builer.setPositiveButton(R.string.impport, new DialogInterface.OnClickListener() {
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

            final TextView confirmed_value = (TextView) v.findViewById(R.id.confirmed_value);
            confirmed_value.setText(new Integer(args.getInt(Constants.EXTRA_INT_CONF)).toString());

            final TextView unconfirmed_value = (TextView) v.findViewById(R.id.unconfirmed_value);
            unconfirmed_value.setText(new Integer(args.getInt(Constants.EXTRA_INT_UNCONF)).toString());

            final TextView draft_value = (TextView) v.findViewById(R.id.draft_value);
            draft_value.setText(new Integer(args.getInt(Constants.EXTRA_INT_DRAFT)).toString());

            final TextView confirmed_balance = (TextView) v.findViewById(R.id.confirmed_balance_value);
            String conf = String.format("%s (%s)",
                    new Long(args.getLong(Constants.EXTRA_LONG_CONF_BAL)),
                    new Integer(args.getInt(Constants.EXTRA_INT_CONF_TXOUTS)));
            confirmed_balance.setText(conf);

            final TextView unconfirmed_balance = (TextView) v.findViewById(R.id.unconfirmed_balance_value);
            String unconf = String.format("%s (%s)",
                    new Long(args.getLong(Constants.EXTRA_LONG_UNCONF_BAL)),
                    new Integer(args.getInt(Constants.EXTRA_INT_UNCONF_TXOUTS)));
            unconfirmed_balance.setText(unconf);

            final TextView network = (TextView) v.findViewById(R.id.network_id_value);
            network.setText( Constants.NETWORK_PARAMETERS.getId() );

            final TextView network_height = (TextView) v.findViewById(R.id.network_height_value);
            network_height.setText(new Long(args.getLong(Constants.EXTRA_LONG_NET_HEIGHT)).toString());

            final TextView local_height = (TextView) v.findViewById(R.id.local_height_value);
            local_height.setText(new Long(args.getLong(Constants.EXTRA_LONG_LOCAL_HEIGHT)).toString());
        }
    }

    public static OverviewFragment newInstance(Bundle args)
    {
        OverviewFragment frag = new OverviewFragment();
        frag.setArguments(args);
        return frag;
    }

}
