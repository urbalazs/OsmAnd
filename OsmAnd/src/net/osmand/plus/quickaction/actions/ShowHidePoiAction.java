package net.osmand.plus.quickaction.actions;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.ContextMenuItem;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.poi.PoiFiltersHelper;
import net.osmand.plus.poi.PoiUIFilter;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionType;
import net.osmand.plus.render.RenderingIcons;
import net.osmand.util.Algorithms;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class ShowHidePoiAction extends QuickAction {

	public static final QuickActionType TYPE = new QuickActionType(5,
			"poi.showhide", ShowHidePoiAction.class)
			.nameActionRes(R.string.quick_action_show_hide_title)
			.nameRes(R.string.poi)
			.iconRes(R.drawable.ic_action_info_dark)
			.category(QuickActionType.CONFIGURE_MAP);

	public static final String KEY_FILTERS = "filters";

	private transient EditText title;

	public ShowHidePoiAction() {
		super(TYPE);
	}

	public ShowHidePoiAction(QuickAction quickAction) {
		super(quickAction);
	}

	@Override
	public String getActionText(OsmandApplication application) {
		String actionName = isActionWithSlash(application) ? application.getString(R.string.shared_string_hide) : application.getString(R.string.shared_string_show);
		return application.getString(R.string.ltr_or_rtl_combine_via_dash, actionName, getName(application));
	}

	@Override
	public boolean isActionWithSlash(OsmandApplication application) {

		return isCurrentFilters(application);
	}

	@Override
	public void setAutoGeneratedTitle(EditText title) {
		this.title = title;
	}

	@Override
	public int getIconRes(Context context) {
		String filtersIdsJson = getParams().get(KEY_FILTERS);
		List<String> filtersIds = tryParseFiltersIds(filtersIdsJson);
		if (Algorithms.isEmpty(filtersIds)) {
			return super.getIconRes();
		}

		OsmandApplication app = (OsmandApplication) context.getApplicationContext();
		PoiUIFilter filter = app.getPoiFilters().getFilterById(filtersIds.get(0));
		if (filter == null) {
			return super.getIconRes();
		}

		Object iconRes = filter.getIconResource();
		return (iconRes instanceof String && RenderingIcons.containsBigIcon(iconRes.toString()))
				? RenderingIcons.getBigIconResourceId(iconRes.toString())
				: super.getIconRes();
	}

	@Override
	public void execute(@NonNull MapActivity mapActivity) {

		mapActivity.closeQuickSearch();

		PoiFiltersHelper pf = mapActivity.getMyApplication().getPoiFilters();
		List<PoiUIFilter> poiFilters = loadPoiFilters(mapActivity.getMyApplication().getPoiFilters());

		if (!isCurrentFilters(pf.getSelectedPoiFilters(), poiFilters)) {

			pf.clearSelectedPoiFilters();

			for (PoiUIFilter filter : poiFilters) {
				if (filter.isStandardFilter()) {
					filter.removeUnsavedFilterByName();
				}
				pf.addSelectedPoiFilter(filter);
			}
		} else {
			pf.clearSelectedPoiFilters();
		}

		mapActivity.getMapLayers().updateLayers(mapActivity);
	}

	private boolean isCurrentFilters(OsmandApplication application) {

		PoiFiltersHelper pf = application.getPoiFilters();
		List<PoiUIFilter> poiFilters = loadPoiFilters(application.getPoiFilters());

		return isCurrentFilters(pf.getSelectedPoiFilters(), poiFilters);
	}

	private boolean isCurrentFilters(Set<PoiUIFilter> currentPoiFilters, List<PoiUIFilter> poiFilters) {
		return currentPoiFilters.size() == poiFilters.size() && currentPoiFilters.containsAll(poiFilters);
	}

	@Override
	public void drawUI(@NonNull ViewGroup parent, @NonNull final MapActivity mapActivity) {
		boolean nightMode = mapActivity.getMyApplication().getDaynightHelper().isNightModeForMapControls();
		View view = UiUtilities.getInflater(mapActivity, nightMode).inflate(R.layout.quick_action_show_hide_poi, parent, false);

		RecyclerView list = (RecyclerView) view.findViewById(R.id.list);
		List<PoiUIFilter> poiFilters = loadPoiFilters(mapActivity.getMyApplication().getPoiFilters());
		final Adapter adapter = new Adapter(poiFilters);
		list.setAdapter(adapter);

		Button addFilter = (Button) view.findViewById(R.id.btnAddCategory);
		addFilter.setOnClickListener(v -> showSingleChoicePoiFilterDialog(mapActivity, adapter));

		parent.addView(view);
	}

	public class Adapter extends RecyclerView.Adapter<Adapter.Holder> {

		private List<PoiUIFilter> filters;

		public Adapter(List<PoiUIFilter> filters) {
			this.filters = filters;
		}

		private void addItem(PoiUIFilter filter) {

			if (!filters.contains(filter)) {

				filters.add(filter);
				savePoiFilters(filters);

				notifyDataSetChanged();
			}
		}

		@Override
		public Adapter.Holder onCreateViewHolder(ViewGroup parent, int viewType) {

			return new Adapter.Holder(LayoutInflater.from(parent.getContext())
					.inflate(R.layout.quick_action_deletable_list_item, parent, false));
		}

		@Override
		public void onBindViewHolder(final Adapter.Holder holder, final int position) {

			final PoiUIFilter filter = filters.get(position);

			Object res = filter.getIconResource();
			if (res instanceof String && RenderingIcons.containsBigIcon(res.toString())) {
				holder.icon.setImageResource(RenderingIcons.getBigIconResourceId(res.toString()));
			} else {
				holder.icon.setImageResource(R.drawable.mx_special_custom_category);
			}

			holder.title.setText(filter.getName());
			holder.delete.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {

					String oldTitle = getTitle(filters);

					filters.remove(position);
					savePoiFilters(filters);

					notifyDataSetChanged();

					String titleContent = title.getText().toString();
					if (oldTitle.equals(titleContent) || titleContent.equals(getName(holder.title.getContext()))) {
						String newTitle = getTitle(filters);
						title.setText(newTitle);
					}
				}
			});
		}

		@Override
		public int getItemCount() {
			return filters.size();
		}

		class Holder extends RecyclerView.ViewHolder {

			private TextView title;
			private ImageView icon;
			private ImageView delete;

			public Holder(View itemView) {
				super(itemView);

				title = (TextView) itemView.findViewById(R.id.title);
				icon = (ImageView) itemView.findViewById(R.id.icon);
				delete = (ImageView) itemView.findViewById(R.id.delete);
			}
		}
	}

	public void savePoiFilters(List<PoiUIFilter> poiFilters) {
		List<String> filters = new ArrayList<>();
		for (PoiUIFilter f : poiFilters) {
			filters.add(f.getFilterId());
		}

		JSONArray jsonArray = new JSONArray(filters);
		getParams().put(KEY_FILTERS, jsonArray.toString());
	}

	private List<PoiUIFilter> loadPoiFilters(PoiFiltersHelper helper) {
		String filtersIds = getParams().get(KEY_FILTERS);
		List<String> filters = tryParseFiltersIds(filtersIds);

		List<PoiUIFilter> poiFilters = new ArrayList<>();
		for (String f : filters) {
			PoiUIFilter filter = helper.getFilterById(f);
			if (filter != null) {
				poiFilters.add(filter);
			}
		}

		return poiFilters;
	}

	private List<String> tryParseFiltersIds(String filtersIdsString) {
		if (Algorithms.isBlank(filtersIdsString)) {
			return Collections.emptyList();
		}
		try {
			List<String> filtersIds = new ArrayList<>();
			JSONArray jsonArray = new JSONArray(filtersIdsString);
			for (int i = 0; i < jsonArray.length(); i++) {
				String filterId = jsonArray.getString(i);
				if (!Algorithms.isBlank(filterId)) {
					filtersIds.add(filterId);
				}
			}
			return filtersIds;
		} catch (JSONException e) {
			return Arrays.asList(filtersIdsString.split(","));
		}
	}

	private void showSingleChoicePoiFilterDialog(final MapActivity mapActivity, final Adapter filtersAdapter) {
		final OsmandApplication app = mapActivity.getMyApplication();
		final PoiFiltersHelper poiFilters = app.getPoiFilters();
		final ContextMenuAdapter adapter = new ContextMenuAdapter(app);

		final List<PoiUIFilter> list = new ArrayList<>();

		for (PoiUIFilter f : poiFilters.getSortedPoiFilters(true)) {
			if (!f.isCustomPoiFilter()) {
				addFilterToList(adapter, list, f);
			}
		}

		boolean nightMode = app.getDaynightHelper().isNightModeForMapControls();
		final ArrayAdapter<ContextMenuItem> listAdapter = adapter.createListAdapter(mapActivity, !nightMode);
		AlertDialog.Builder builder = new AlertDialog.Builder(UiUtilities.getThemedContext(mapActivity, nightMode));
		builder.setAdapter(listAdapter, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {

				String oldTitle = getTitle(filtersAdapter.filters);

				filtersAdapter.addItem(list.get(which));

				String titleContent = title.getText().toString();
				if (oldTitle.equals(titleContent) || titleContent.equals(getName(mapActivity))) {
					String newTitle = getTitle(filtersAdapter.filters);
					title.setText(newTitle);
				}
			}

		});
		builder.setTitle(R.string.show_poi_over_map);
		builder.setNegativeButton(R.string.shared_string_dismiss, null);

		final AlertDialog alertDialog = builder.create();

		alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
			@Override
			public void onShow(DialogInterface dialog) {
				Button neutralButton = alertDialog.getButton(DialogInterface.BUTTON_NEUTRAL);
				Drawable drawable = app.getUIUtilities().getThemedIcon(R.drawable.ic_action_multiselect);
				neutralButton.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null);
			}
		});

		alertDialog.show();
	}

	private String getTitle(List<PoiUIFilter> filters) {

		if (filters.isEmpty()) return "";

		return filters.size() > 1
				? filters.get(0).getName() + " +" + (filters.size() - 1)
				: filters.get(0).getName();
	}

	private void addFilterToList(final ContextMenuAdapter adapter,
								 final List<PoiUIFilter> list,
								 final PoiUIFilter f) {
		list.add(f);
		ContextMenuItem.ItemBuilder builder = new ContextMenuItem.ItemBuilder();

		builder.setTitle(f.getName());

		if (RenderingIcons.containsBigIcon(f.getIconId())) {
			builder.setIcon(RenderingIcons.getBigIconResourceId(f.getIconId()));
		} else {
			builder.setIcon(R.drawable.mx_special_custom_category);
		}

		builder.setSkipPaintingWithoutColor(true);
		adapter.addItem(builder.createItem());
	}

	@Override
	public boolean fillParams(@NonNull View root, @NonNull MapActivity mapActivity) {
		return !getParams().isEmpty() && (getParams().get(KEY_FILTERS) != null || !getParams().get(KEY_FILTERS).isEmpty());
	}
}
