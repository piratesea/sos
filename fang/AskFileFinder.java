package com.radar.fang;

import java.util.List;

import com.google.common.collect.Lists;
import com.radar.base.atoz.*;
import com.radar.base.parts.fileSystem.*;
import com.radar.base.parts.search.LocalFileSearcher;
import com.radar.base.parts.stat.*;
import com.radar.base.parts.tuple.StrObjItem;
import com.radar.base.ask.AskSession;
import com.radar.base.ask.KunBase;
import com.radar.base.parts.tuple.StrStrItem;

public class AskFileFinder extends KunBase {

	@Override
	public List<StrObjItem> getUsage() {
		List<StrObjItem> items = Lists.newArrayList();
		List<String> findLines = Lists.newArrayList();
		findLines.add("search files with criteria, sorting and display file items (also text lines if possible)");
		findLines.add("+or, false, to apply many conditions, file search or text search, default to be 'and'");
		findLines.add("from=<groupName> folders associated to given group defined in config folderGroup.fish");
		findLines.add("from=<dirAce> from=<dirBob>, we can search from many folders");
		findLines.add("related: <fileSorter> <filePath> <fileItemsDisplay> <manyLines>");
		findLines.add("for stat, default limit is 50");
		findLines.add("sort=<xxx>, " + FileSorter.allowedSorterRegex);
		for(StrStrItem ssItem : LocalFileSearcher.allowedParams) {
			findLines.add(ssItem.getFirst() + ", " + ssItem.getSecond());
		}
		items.add(StrObjItem.of("find", findLines));
		return items;
	}

	@Override
	public boolean ask() {
		if (leaderIs("find")) {
			String expression = Q.of(rawInput).replaceSpecifiedWordOnlyOnce(leader).trim();

			String singleSearchBase = "";
			List<String> manyLocations = Lists.newArrayList();
			if (hasParam("from")) {
				List<String> sourceList = listParamOf("from");
				for(String source : sourceList) {
					String configName = ("folderGroup." + source);
					List<String> yourFolders = AskSession.getConfig().listParamOf(configName, false);
					if(L.good(yourFolders)) {
						for (String yourFolder : yourFolders) {
							X.makeSureIsFolder(yourFolder);
							manyLocations.add(yourFolder);
						}
						continue;
					}

					String hisFolder = tryRelativeOrAbsoluteOf(source);
					X.makeSureIsFolder(hisFolder);
					manyLocations.add(hisFolder);
				}
			} else {
				manyLocations.add(pwd());
			}

			if(L.justOne(manyLocations)) {
				singleSearchBase = manyLocations.getFirst();
			}

			LocalFileSearcher fisher = LocalFileSearcher.of(manyLocations);
			List<LocalFileItem> wantedFiles = fisher.searchByExpression(expression);

			if (L.bad(wantedFiles)) {
				C.say(K.noResultsFound);
				return markTrue();
			}

			String beenSortedBy = fisher.getBeenSortedBy();
			String yourSorter = stringParamOf("sort");
			if (S.good(yourSorter)) {
				if (S.diff(yourSorter, beenSortedBy)) {
					wantedFiles.sort(KunComparators.forLocalFileItem(yourSorter));
				}
			} else {
				if (S.bad(beenSortedBy)) {
					wantedFiles.sort(KunComparators.forLocalFileItem(FileSorter.timeDescend));
				}
			}

			String displayParams = rawInput;
			if(S.same(beenSortedBy, FileSorter.timeDescend) && !hasParam("time")) {
				displayParams += " +time";
			}
			ManyLocalFileItems many = ManyLocalFileItems.of(wantedFiles);
			many.setParentPath(singleSearchBase);
			Integer limit = intParamOf("limit", null);
			if(hasParam(LocalFileSearcher.nameLike)) {
				many.sortBySimScore();
				if(O.bad(limit)) limit = 50;
			}
			if(flagParamExistsAndTrue(LocalFileSearcher.cStat)) {
				many.applyCommentStatAndSort();
				if(O.bad(limit)) limit = 50;
			}
			many.applyLimit(limit);
			if (toHtml()) {
				String info = S.format("searchBase: {} , params: {}", singleSearchBase, expression);
				exportHtmlTable(many.displayTableByParams(displayParams).setTableInfo(info));
			} else {
				exportLines(many.displayLinesByParams(displayParams));
			}

			return markTrue();
		}

		return false;
	}
}