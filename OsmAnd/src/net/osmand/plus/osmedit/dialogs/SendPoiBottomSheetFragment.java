package net.osmand.plus.osmedit.dialogs;

import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.osmedit.OpenstreetmapPoint;
import net.osmand.plus.osmedit.OsmPoint;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.util.Algorithms;

public class SendPoiBottomSheetFragment extends MenuBottomSheetDialogFragment {

    public static final String TAG = "SendPoiBottomSheetFragment";
    public static final String OPENSTREETMAP_POINT = "openstreetmap_point";
    public static final String POI_UPLOADER_TYPE = "poi_uploader_type";
    private OsmPoint[] poi;

    protected OsmandSettings settings;

    public enum PoiUploaderType {
        SIMPLE,
        FRAGMENT
    }

    protected OsmandApplication getMyApplication() {
        return (OsmandApplication) getActivity().getApplication();
    }

    private boolean isLoginOAuth() {
        return !Algorithms.isEmpty(getMyApplication().getSettings().USER_DISPLAY_NAME.get());
    }

    @Override
    public void createMenuItems(Bundle savedInstanceState) {
        final boolean isNightMode = getMyApplication().getDaynightHelper().isNightModeForMapControls();
        final View sendOsmPoiView = View.inflate(new ContextThemeWrapper(getContext(), themeRes), R.layout.send_poi_fragment, null);
        final SwitchCompat closeChangset = sendOsmPoiView.findViewById(R.id.close_change_set_checkbox);
        final TextView accountName = sendOsmPoiView.findViewById(R.id.user_name);
        settings = getMyApplication().getSettings();
        String userNameOAuth = settings.USER_DISPLAY_NAME.get();
        String userNameOpenID = settings.USER_NAME.get();
        String userName = isLoginOAuth() ? userNameOAuth : userNameOpenID;
        accountName.setText(userName);
        closeChangset.setBackgroundResource(isNightMode ? R.drawable.layout_bg_dark : R.drawable.layout_bg);
        closeChangset.setPadding(30, 0, 0, 0);
        closeChangset.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isNightMode) {
                    closeChangset.setBackgroundResource(isChecked ? R.drawable.layout_bg_dark_solid : R.drawable.layout_bg_dark);
                } else {
                    closeChangset.setBackgroundResource(isChecked ? R.drawable.layout_bg_solid : R.drawable.layout_bg);
                }
                closeChangset.setPadding(30, 0, 0, 0);
            }
        });
        final SimpleBottomSheetItem titleItem = (SimpleBottomSheetItem) new SimpleBottomSheetItem.Builder()
                .setCustomView(sendOsmPoiView)
                .create();
        items.add(titleItem);
    }

    public static SendPoiBottomSheetFragment showInstance(@NonNull OsmPoint[] points, @NonNull SendPoiBottomSheetFragment.PoiUploaderType uploaderType) {
            SendPoiBottomSheetFragment fragment = new SendPoiBottomSheetFragment();
            Bundle bundle = new Bundle();
            bundle.putSerializable(OPENSTREETMAP_POINT, points);
            bundle.putString(POI_UPLOADER_TYPE, uploaderType.name());
            fragment.setArguments(bundle);
            return fragment;
        }

    @Override
    protected UiUtilities.DialogButtonType getRightBottomButtonType() {
        return (UiUtilities.DialogButtonType.PRIMARY);
    }

    @Override
    protected void onRightBottomButtonClick() {
        View view = getView();
        poi = (OsmPoint[]) getArguments().getSerializable(OPENSTREETMAP_POINT);
        final SwitchCompat closeChangeSetCheckBox =
                view.findViewById(R.id.close_change_set_checkbox);
        final EditText messageEditText = view.findViewById(R.id.message_field);
        final SendPoiDialogFragment.PoiUploaderType poiUploaderType = SendPoiDialogFragment.PoiUploaderType.valueOf(getArguments().getString(POI_UPLOADER_TYPE, SendPoiDialogFragment.PoiUploaderType.SIMPLE.name()));
        final SendPoiDialogFragment.ProgressDialogPoiUploader progressDialogPoiUploader;
        if (poiUploaderType == SendPoiDialogFragment.PoiUploaderType.SIMPLE && getActivity() instanceof MapActivity) {
            progressDialogPoiUploader =
                    new SendPoiDialogFragment.SimpleProgressDialogPoiUploader((MapActivity) getActivity());
        } else {
            progressDialogPoiUploader = (SendPoiDialogFragment.ProgressDialogPoiUploader) getParentFragment();
        }
        if (progressDialogPoiUploader != null) {
            String comment = messageEditText.getText().toString();
            if (comment.length() > 0) {
                for (OsmPoint osmPoint : poi) {
                    if (osmPoint.getGroup() == OsmPoint.Group.POI) {
                        ((OpenstreetmapPoint) osmPoint).setComment(comment);
                        break;
                    }
                }
            }
            progressDialogPoiUploader.showProgressDialog(poi,
                    closeChangeSetCheckBox.isChecked(),
                    false);
        }
    dismiss();
}

    @Override
    protected int getRightBottomButtonTextId() {
        return R.string.shared_string_upload;
    }

}

