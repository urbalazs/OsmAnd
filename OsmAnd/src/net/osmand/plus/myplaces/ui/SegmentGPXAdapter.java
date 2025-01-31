package net.osmand.plus.myplaces.ui;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;

import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.track.helpers.GpxSelectionHelper.GpxDisplayItem;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.track.helpers.TrackDisplayHelper;
import net.osmand.plus.views.controls.PagerSlidingTabStrip;
import net.osmand.plus.views.controls.WrapContentHeightViewPager;

import java.util.List;

public class SegmentGPXAdapter extends ArrayAdapter<GpxDisplayItem> {

	private OsmandApplication app;
	private TrackDisplayHelper displayHelper;
	private SegmentActionsListener listener;
	private boolean nightMode;

	public SegmentGPXAdapter(@NonNull Context context, @NonNull List<GpxDisplayItem> items,
							 @NonNull TrackDisplayHelper displayHelper,
							 @NonNull SegmentActionsListener listener,
							 boolean nightMode) {
		super(context, R.layout.gpx_list_item_tab_content, items);
		this.app = (OsmandApplication) context.getApplicationContext();
		this.displayHelper = displayHelper;
		this.listener = listener;
		this.nightMode = nightMode;
	}

	@Override
	public boolean isEmpty() {
		return false;
	}

	@NonNull
	@Override
	public View getView(int position, View convertView, @NonNull ViewGroup parent) {
		View row = convertView;
		boolean create = false;
		if (row == null) {
			create = true;
			row = createGpxTabsView(parent, nightMode);
		}
		GpxDisplayItem item = getItem(position);
		if (item != null) {
			WrapContentHeightViewPager pager = row.findViewById(R.id.pager);
			PagerSlidingTabStrip tabLayout = row.findViewById(R.id.sliding_tabs);

			pager.setAdapter(new GPXItemPagerAdapter(app, item, displayHelper, nightMode, listener, false, false));
			if (create) {
				tabLayout.setViewPager(pager);
			} else {
				tabLayout.notifyDataSetChanged(true);
			}
		}
		return row;
	}

	public static View createGpxTabsView(ViewGroup root, boolean nightMode) {
		Context context = root.getContext();
		View tabsView = UiUtilities.getInflater(context, nightMode)
				.inflate(R.layout.gpx_list_item_tab_content, root, false);
		setupGpxTabsView(tabsView, nightMode);
		return tabsView;
	}

	public static void setupGpxTabsView(View tabsView, boolean nightMode) {
		Context context = tabsView.getContext();

		PagerSlidingTabStrip tabLayout = tabsView.findViewById(R.id.sliding_tabs);
		tabLayout.setTabBackground(AndroidUtils.resolveAttribute(context, R.attr.btn_bg_border_inactive));
		tabLayout.setDividerWidth(AndroidUtils.dpToPx(context, 1));
		tabLayout.setDividerColor(ColorUtilities.getStrokedButtonsOutlineColor(context, nightMode));
		tabLayout.setIndicatorHeight(0);
		tabLayout.setShouldExpand(true);

		WrapContentHeightViewPager pager = tabsView.findViewById(R.id.pager);
		pager.setSwipeable(false);
		pager.setOffscreenPageLimit(2);
	}
}
