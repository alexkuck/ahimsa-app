package io.ahimsa.ahimsa_app.ui;

import android.os.Bundle;
import android.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import io.ahimsa.ahimsa_app.Constants;
import io.ahimsa.ahimsa_app.R;
import io.ahimsa.ahimsa_app.core.Utils;


public class CreateBulletinFragment extends Fragment {

    private TextView messageCount;
    private TextView topicCount;
    private TextView estimatedCost;

    private int msg_count;
    private int top_count;

    public static String EXTRA_STRING_TOPIC = "string_topic";
    public static String EXTRA_STRING_MESSAGE = "string_estimated_cost";

    public CreateBulletinFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_create_bulletin, container, false);
        updateView(v, getArguments());

        estimatedCost = (TextView) v.findViewById(R.id.estimated_cost_value);
        estimatedCost.setText(Utils.commarizer(String.valueOf(Constants.MIN_FEE)));

        EditText topicEditText = (EditText) v.findViewById(R.id.topic_edit_text);
        topicCount = (TextView) v.findViewById(R.id.topic_count_value);
        topicCount.setText(String.valueOf(Constants.MAX_TOPIC_LEN));

        final TextWatcher topicCountWatcher= new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
                topicCount.setText(String.valueOf(Constants.MAX_TOPIC_LEN - s.length()));
                setTopicCount(s.length());
            }

            public void afterTextChanged(Editable s) {}
        };
        topicEditText.addTextChangedListener(topicCountWatcher);


        final EditText messageEditText = (EditText) v.findViewById(R.id.message_edit_text);
        messageCount = (TextView) v.findViewById(R.id.message_count_value);
        messageCount.setText(String.valueOf(Constants.MAX_MESSAGE_LEN));

        final TextWatcher messageCountWatcher= new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
                messageCount.setText(String.valueOf(Constants.MAX_MESSAGE_LEN - s.length()));
                setMessageCount(s.length());
            }

            public void afterTextChanged(Editable s) {}
        };
        messageEditText.addTextChangedListener(messageCountWatcher);


        return v;
    }

    private void setTopicCount(int len) {
        top_count = len;
        setEstimatedCost();
    }

    private void setMessageCount(int len) {
        msg_count = len;
        setEstimatedCost();
    }

    private void setEstimatedCost() {
        int x = (top_count + msg_count) % Constants.CHAR_PER_OUT;
        if(x == 0 || x == 1 || x == 19) {
            String est = String.valueOf(Utils.getEstimatedCost(Constants.MIN_FEE, Constants.MIN_DUST, top_count, msg_count));
            estimatedCost.setText(Utils.commarizer(est));
        }
    }

    public void updateView(Bundle args) {
        updateView(getView(), args);
    }

    private void updateView(View v, Bundle args) {
        if(v != null) {

            final TextView address_value = (TextView) v.findViewById(R.id.address_value);
            String address = args.getString(Constants.EXTRA_STRING_ADDRESS);
            if(address.length() > 8) {
                address_value.setText(address.substring(1,9));
            }

            Long confirmed_balance = args.getLong(Constants.EXTRA_LONG_AVAILABLE_BAL);
            final TextView confirmed_value = (TextView) v.findViewById(R.id.confirmed_balance_value);
            confirmed_value.setText( Utils.commarizer(confirmed_balance.toString()) );

        }
    }

    public void addEntityHash(String hash64)
    {
        EditText message_edit = (EditText) getView().findViewById(R.id.message_edit_text);
        message_edit.setText(message_edit.getText() + "\n![description](img.ahimsa.io/" + hash64.toString().trim() + ")");
    }



    public static CreateBulletinFragment newInstance(Bundle args) {
        CreateBulletinFragment frag = new CreateBulletinFragment();
        frag.setArguments(args);
        return frag;
    }

    public Bundle getBulletinBundle() {
        Bundle bundle_of_fun = new Bundle();

        EditText topic_edit = (EditText) getView().findViewById(R.id.topic_edit_text);
        EditText message_edit = (EditText) getView().findViewById(R.id.message_edit_text);
        bundle_of_fun.putString(EXTRA_STRING_TOPIC, topic_edit.getText().toString());
        bundle_of_fun.putString(EXTRA_STRING_MESSAGE, message_edit.getText().toString());

        return bundle_of_fun;
    }


}
