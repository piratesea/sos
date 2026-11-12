package com.radar.fang;

import java.io.File;
import java.util.List;

import com.google.common.collect.Lists;
import com.radar.base.misc.Aregex;
import com.radar.base.atoz.*;
import com.radar.base.parts.cmd.CmdResultHolder;
import com.radar.base.parts.fileSystem.*;
import com.radar.base.parts.fileSystem.sync.SyncFromLocalToLocal;
import com.radar.base.parts.search.LocalFileSearcher;
import com.radar.base.parts.search.StringSimItem;
import com.radar.base.parts.simSearch.SimHelper;
import com.radar.base.parts.stat.*;
import com.radar.base.parts.tuple.*;
import com.radar.base.ask.AskSession;
import com.radar.base.ask.KunBase;

public class AskFileSystem extends KunBase {

	@Override
	public List<StrObjItem> getUsage() {
		List<StrObjItem> items = Lists.newArrayList();
		{
			List<String> lines = Lists.newArrayList();
			lines.add("s=<sorterName>, value would be like: na, pa, td, sd");
			lines.add("name, simple file name");
			lines.add("path, absolute file path");
			lines.add("time, last modified time");
			lines.add("size, file size for normal file, sub files count for folder, folder bigger than file");
			lines.add("ver, version info in name, like guava-1.4.12.jar");
			lines.add("sim, similarity score, s=sim-d, applicable in fuzzy search");
			items.add(StrObjItem.of("<fileSorter>", lines));
		}
		{
			List<String> lines = Lists.newArrayList();
			lines.add("filePath could contain space without double quotes");
			lines.add("ends with <, open file in Radar web page");
			lines.add("ends with <<, open file with default opener or Resource Manager");
			lines.add("+web, false, open file in Radar web page");
			lines.add("+win, false, open file with Windows default opener or Resource Manager");
			lines.add("+remove, false, open file with default opener or Resource Manager");
			lines.add("cto=<folder>, copy file to");
			lines.add("related <fileItemsDisplay>");
			items.add(StrObjItem.of("<filePath>", lines));
		}
		{
			List<String> lines = Lists.newArrayList();
			lines.add("+time, show last modified time");
			lines.add("+size, cozy file size info for normal file, subs for folder");
			lines.add("+lines, total lines of text file");
			lines.add("+pages, total pages of files like docx, ppt, pdf and so on");
			lines.add("+entries, total entries of files like jar and so on");
			lines.add("+bytes, total bytes of normal file");
			lines.add("+md5, md5 value of normal file, be careful of big file");
			lines.add("+sam, sample md5 value of normal file, be careful of big file");
			lines.add("+ziprate, show compression rate of file if zipped");
			lines.add("+full, show full file path, default relative path if applicable");
			lines.add("+more, show file size and count of lines if text file");
			lines.add("show sim score if has nameLike=apple");
			items.add(StrObjItem.of("<fileItemsDisplay>", lines));
		}
		items.add(StrObjItem.of("ll", "list direct sub files in pwd or your folder, sorted by time descend"));
		items.add(StrObjItem.of("dall", "list all deep sub files in pwd or your folder, sorted by path ascend"));
		{
			List<String> lines = Lists.newArrayList();
			lines.add("for single file like xxx.tar.gz, +x to expand via tar -xzvf <filepath>, create new folder if whereToLocation is empty");
			lines.add("for single file like xxx.tar.gz, when x=false, just to view items inside only via tar -vt <filepath>");
			lines.add("for single file like xxx.tar.gz, +newdir, create new folder if whereToLocation is empty");
			lines.add("for other single file or folder, compress to .tar.gz, via tar -czvf good.tar.gz <filepath>");
			lines.add("tar&zip files from <manyLines> to specified location by param w=<destination> via tar -czvf");
			items.add(StrObjItem.of("take", lines));
		}
		{
			List<String> lines = Lists.newArrayList();
			lines.add("pwd, default to be <basePath> in config or <askHome> as ~/askHome");
			lines.add("solo, change pwd to <basePath> in config or <askHome> as ~/askHome");
			lines.add("when there is only one folder, get into that folder");
			lines.add("when your wanted folder name is equal to one specific folder, get into");
			lines.add("do fuzzy search against sub folders, pick the most likely one or show candidates");
			items.add(StrObjItem.of("cd", lines));
		}
		{
			List<String> lines = Lists.newArrayList();
			lines.add("Create one or multiple subdirectories under the current working directory (pwd)");
			lines.add("ace bob carrie – space-separated names create three independent subfolders");
			lines.add("\"ace bob carrie\" – quoted input creates a single folder named \"ace bob carrie\" (with spaces in name)");
			lines.add("ace/bob/carrie – nested path syntax creates a recursive directory tree: ace → ace/bob → ace/bob/carrie");
			items.add(StrObjItem.of("mkdir", lines));
		}
//		{
//			List<String> lines = Lists.newArrayList();
//			lines.add("sync files between local folders");
//			lines.add("sourceFolder=");
//			lines.add("destinFolder=");
//			lines.add("+apply, false: dry run, check only; true: apply changes if any");
//			lines.add("example>> local-sync sourceFolder=C:/code/good destinFolder=C:/archive");
//			items.add(StrObjItem.of("local-sync", lines));
//		}
		{
			List<String> lines = Lists.newArrayList();
			lines.add("collect updated files from somewhere against baseline");
			lines.add("collect-updated from=C:\\data-mesh baseline=C:\\baseline-580-files.txt");
			items.add(StrObjItem.of("collect-updated", lines));
		}

		{
			List<String> lines = Lists.newArrayList();
			lines.add("handle a specified set of files listed in text file, or from previous command");
			lines.add("parent=<parentFolder>, lines could be relative filepath");
			lines.add("+remove, remove files");
			lines.add("+cto, copy files to, ");
			lines.add("+raw, display raw result");
			lines.add("+show, show good files info");
			lines.add("stat=type, stat of file extension names");
			lines.add("stat=subs, stat of every dir and its count of sub files");
			lines.add("dup=name, stat of files with same name");
			lines.add("dup=size, stat of files with same size");
			lines.add("dup=sam, stat of files with same fingerprint");
			items.add(StrObjItem.of("files", lines));
		}

		return items;
	}

	@Override
	public boolean ask() {

		if(leaderIs("==")) {
			openLocalFileWithRadarWeb(pwd());
			return markTrue();
		}

		if(leaderIs("===")) {
			openLocalFileViaWindowsOpener(pwd());
			return markTrue();
		}

		if (F.exists(nativeCommandWithoutParams)) {
			String filepath = F.resolvePath(nativeCommandWithoutParams);
			X.makeSureGood(filepath, nativeCommandWithoutParams);
			reportFileIfNeeded(filepath);
			ManyLocalFileItems many = ManyLocalFileItems.ofFiles(Lists.newArrayList(F.of(filepath)));

			if(flagParamExistsAndTrue("remove")) {
				many.remove();
				return markTrue();
			}

			String copyTo = stringParamOf("cto");
			if(S.good(copyTo)) {
				String whereToSave = tryRelativeOrAbsoluteOf(copyTo);
				X.makeSureIsFolder(whereToSave);
				StrStrItem location = many.copyTo(whereToSave, true,false);
				openLocalFileWithRadarWeb(location.getSecond());

				return markTrue();
			}

			exportLines(many.displayLinesByParams(rawInput));

			return markTrue();
		}

		String regex = "(.+?)\\s+(={2,3})";
		List<String> players = Aregex.of(regex).getWholeMatchForManyGroups(nativeCommandWithoutParams);
		if(L.good(players)) {
			String filepath = L.firstOne(players);
			X.makeSureFileExists(filepath);
			String bob = L.lastOne(players);
			if(S.lengthIs(bob, 2)) {
				openLocalFileWithRadarWeb(filepath);
			} else {
				Afile.of(filepath).openInWindows();
			}
			return markTrue();
		}

		if (leaderIs("ll")) {
			String searchBase = pwd();
			if (S.good(remain)) {
				searchBase = tryRelativeOrAbsoluteOf(remain);
				X.makeSureIsFolder(searchBase);
			}

			List<File> files = LocalFileSearcher.of(searchBase).listDirectSubFiles();
			files.sort(KunComparators.forRegularFile(sorter(FileSorter.timeDescend)));
			ManyLocalFileItems many = ManyLocalFileItems.ofFiles(files);
			many.setParentPath(searchBase);
			myNameInfo = S.format("sub-{}-files-in-{}", files.size(), P.filenameOf(searchBase));

			if (toHtml()) {
				String info = S.format("list direct subs create: {}", searchBase);
				exportHtmlTable(many.displayTableByParams(rawInput).setTableInfo(info));
			} else {
				exportLines(many.displayLinesByParams(rawInput));
			}

			return markTrue();
		}

		if (leaderIs("dall")) {
			String searchBase = pwd();
			if (S.good(remain)) {
				searchBase = tryRelativeOrAbsoluteOf(remain);
				X.makeSureIsFolder(searchBase);
			}
			List<File> files = LocalFileSearcher.of(searchBase).listDeepSubFiles();
			files.sort(KunComparators.forRegularFile(sorter(FileSorter.pathAscend)));
			ManyLocalFileItems many = ManyLocalFileItems.ofFiles(files);
			many.setParentPath(searchBase);
			if (toHtml()) {
				String info = S.format("show deep subs create: {}", searchBase);
				exportHtmlTable(many.displayTableByParams(rawInput).setTableInfo(info));
			} else {
				exportLines(many.displayLinesByParams(rawInput));
			}

			return markTrue();
		}

		if(leaderIs("targz")) {
			String source = tryRelativeOrAbsoluteOf(stringParamOf("source", true));
			X.makeSureFileExists(source);
			String destin;
			if(hasParam("destin")) {
				destin = stringParamOf("destin");
				X.makeSureIsFolder(destin);
			} else {
				destin = pwd();
			}
			String aceFilename = P.filenameOf(source);
			String bobFilename = aceFilename + ".tar.gz";
			String bobFilepath = P.conn(destin, bobFilename);
			String parentFolder = P.parentOf(source);
			String cmd = S.format("tar -czf {} {}", S.xquote(bobFilepath), S.xquote(aceFilename));
			D.say(cmd);
			E.local().fromDirectory(parentFolder).executeWithSystemOutPrint(cmd);
			openLocalFileWithRadarWeb(bobFilepath);

			return markTrue();
		}

		if (leaderIs("take")) {
			X.showMe(">> disabled since 525");
			List<String> many = getManyLinesInput();
			String whereToSave = pwd();
			if(hasParam("w")) {
				whereToSave = tryRelativeOrAbsoluteOf(stringParamOf("w"));
				X.makeSureIsFolder(whereToSave);
			}
			if(L.good(many)) {
				// treat as files, to tar and zip
				String tarFilename = S.format("{}-{}-{}-items{}{}", W.MMddWHHmm(), R.name(), many.size(), K.dotTar, K.dotGz);
				X.makeSureIsFolder(whereToSave);
				String tarFilepath = P.conn(whereToSave, tarFilename);
				C.say("Saved at: " + tarFilepath);
				StringBuffer cmd = S.sb("tar -czvf");
				cmd.append(" ").append(tarFilepath);
				int count = 0;
				for(String filepath : many) {
					if(F.bad(filepath)) {
						C.say(">> ignored, not valid file: " + filepath);
						continue;
					}
					count++;
					cmd.append(" ").append(S.xquote(filepath));
				}

				if(count == 0) {
					C.say(">> no valid file provided, leave");
				} else {
					CmdResultHolder mom = E.local().execute(cmd.toString());
					exportLines(mom.getAnySolid());
					openLocalFileWithRadarWeb(tarFilepath);
				}

				return markTrue();
			}

			String single = remain;
			if(S.bad(single)) {
				X.showMe("expect something to take");
			}

			String wantedFilepath = tryRelativeOrAbsoluteOf(single);
			X.makeSureFileExists(wantedFilepath);

			String filename = P.filenameOf(wantedFilepath);
			if (S.contains(filename, K.dotGz) && S.contains(filename, K.dotTar)) {
				if (flagParamOf("x", false)) {
					// to extract
					boolean solid = Afolder.of(whereToSave).countOfDirectSubs() > 0;
					if (solid && flagParamOf("newdir", true)) {
						String newFolderName = S.format("{}-for-unzip-of-{}", W.MMddWHHmm(), filename);
						whereToSave = P.conn(whereToSave, newFolderName);
						Afolder.of(whereToSave).makeDirectoriesIfNeeded();
					}
					String cmd = S.format("tar -xzvf {} -C {}", S.xquote(wantedFilepath), S.xquote(whereToSave));
					CmdResultHolder mom = E.local().execute(cmd);
					exportLines(mom.getAnySolid());
					openLocalFileWithRadarWeb(whereToSave);
				} else {
					// to view
					String cmd = S.format("tar -tf {}", S.xquote(wantedFilepath));
					CmdResultHolder mom = E.local().execute(cmd);
					exportLines(mom.getAnySolid());
				}

				return markTrue();
			}

			{
				// treat as files, to tar and zip
				String wantedFilename = P.filenameOf(wantedFilepath);
				String goodName = S.removeSpace(wantedFilename);
				String tarFilename = S.format("{}-{}{}{}", W.MMddWHHmm(), goodName, K.dotTar, K.dotGz);
				X.makeSureIsFolder(whereToSave);
				String tarFilepath = P.conn(whereToSave, tarFilename);
				C.say("Saved at: " + tarFilepath);
				StringBuffer cmd = S.sb("tar -czvf");
				cmd.append(" ").append(tarFilepath);
				cmd.append(" ").append(S.xquote(wantedFilepath));

				CmdResultHolder mom = E.local().execute(cmd.toString());
				exportLines(mom.getAnySolid());
				openLocalFileWithRadarWeb(tarFilepath);

				return markTrue();
			}
		}

		if (leaderIs("cd")) {
			String targetDir = AskSession.basePath();
			if (S.good(remain)) {
				if (S.hasSame(remain, ".", "..") || P.containsSeparator(remain)) {
					targetDir = tryRelativeOrAbsoluteOf(remain);
				} else {
					List<String> directSubFoldersNames = LocalFileSearcher.of(pwd()).listDirectSubFolders().stream()
						.map(File::getName).toList();
					if (L.bad(directSubFoldersNames)) {
						C.say(">> well, no folder to open");
						return markTrue();
					}

					if (L.justOne(directSubFoldersNames)) {
						String cat = L.firstOne(directSubFoldersNames);
						C.say(">> there is one only folder [{}]", cat);
						targetDir = P.conn(pwd(), cat);
					} else {
						// try similarity
						List<StringSimItem> items = SimHelper.scoreSimilarity(directSubFoldersNames, remain);
						if (flagParamOf("score", false)) {
							D.list(items);
						}
						List<StringSimItem> topGroup = SimHelper.topGroup(items);
						if (L.moreThanOne(topGroup)) {
							C.say(">> you might want to check ...");
							C.list(topGroup.stream().map(StringSimItem::getRawString).toList(), true);
							return markTrue();
						}

						StringSimItem first = L.firstOne(topGroup);
						if (first.getScore() > 100) {
							if (first.getScore() < SimHelper.looseSame) {
								C.say(">> you might want to open [{}]", first.getRawString());
							}
							targetDir = P.conn(pwd(), first.getRawString());
						} else {
							C.say(">> well, sub folders had been sorted for you ...");
							C.list(items.stream().map(StringSimItem::getRawString).toList(), true);
							return markTrue();
						}
					}
				}
			}

			String goodPath = P.localPath(targetDir);
			X.makeSureIsFolder(goodPath);
			AskSession.setPwd(goodPath);
			C.say("directory changed to: ");
			return exportLines(List.of(goodPath));
		}

		if(leaderIs("mkdir")) {
			makeSureHasParam("sub directory you want to create");
			String subFolderInfo = remain;
			KunFolderCreator kun = KunFolderCreator.of(subFolderInfo);
			String result = kun.createFrom(pwd());
			if(S.bad(result)) {
				C.say(">> nothing happened");
			} else {
				C.say(">> sub folder(s) available, dreams come true");
				AskSession.setPwd(result);
			}
			
			return markTrue();
		}

		if (leaderIs("local-sync")) {
			// sink from local to local
			boolean apply = flagParamExistsAndTrue(K.apply);
//			boolean parentsFlag = flagParamOf("flagWithParents", true);
			String sourceFolder = stringParamOf(K.sourceFolder, true);
			String destinFolder = pwd();
			if (hasParam(K.destinFolder)) {
				destinFolder = tryRelativeOrAbsoluteOf(stringParamOf(K.destinFolder));
				X.makeSureIsFolder(destinFolder);
			}

			try {
				SyncFromLocalToLocal xxx = SyncFromLocalToLocal.of(sourceFolder, destinFolder);
//				xxx.flagWithParents(parentsFlag);
				List<String> infoHolder = apply ? xxx.checkAndApply() : xxx.checkOnly();
				C.list(infoHolder, false);
				String actualDestinFolderPath = xxx.getActualDestinFolderPath();
				C.say("wanted: {}", actualDestinFolderPath);
				if(apply) {
					super.openLocalFileWithRadarWeb(actualDestinFolderPath);
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}

			return markTrue();
		}

		if(leaderIs("collect-updated")) {
			C.say(">> collect updated files >>");
			String from;
			{
				String temp = stringParamOf("from", true);
				from = tryRelativeOrAbsoluteOf(temp);
				X.makeSureIsFolder(from);
			}

			String baseline;
			{
				String temp = stringParamOf("baseline", true);
				baseline = tryRelativeOrAbsoluteOf(temp);
				X.makeSureIsNormalFile(baseline);
			}

			String whereToSave;
			{
				String temp = stringParamOf("w", null);
				if(S.good(temp)) {
					whereToSave = tryRelativeOrAbsoluteOf(temp);
					X.makeSureIsFolder(baseline);
				} else {
					whereToSave = pwd();
				}
			}
			C.say(">> fromFolder: " + from);
			C.say(">> baseline: " + baseline);
			C.say(">> whereToSave: " + whereToSave);
			String actualDestin = UpdatedFilesCollector.of(from).setBaselineFileItemsPath(baseline).copyTo(whereToSave);
			C.say(">> saved at {}", actualDestin);
			openLocalFileWithRadarWeb(actualDestin);

			return markTrue();
		}

		if(leaderIs("files")) {
			List<String> lines = getManyLinesInput();
			List<LocalFileItem> goodFiles = Lists.newArrayList();
			List<String> badFiles = Lists.newArrayList();
			for(String line : lines) {
				if(S.weak(line)) {
					continue;
				}
				String filepath = KunFilesDiffer.fetchFilepathOnly(line);
				if(F.exists(filepath)) {
					goodFiles.add(LocalFileItem.of(filepath));
					continue;
				}

				String tempPath = tryRelativeOrAbsoluteOf(filepath);
				if(F.exists(tempPath)) {
					goodFiles.add(LocalFileItem.of(tempPath));
					continue;
				}

				badFiles.add(filepath);
			}

			if(L.good(badFiles)) {
				String tempParent = stringParamOf("parent");
				for(String filepath : Lists.newArrayList(badFiles)) {
					if(S.good(tempParent)) {
						String actualParent = tryRelativeOrAbsoluteOf(tempParent);
						String newPath = P.connForWin(actualParent, filepath);
						badFiles.remove(filepath);
						if(F.exists(newPath)) {
							goodFiles.add(LocalFileItem.of(newPath));
						} else {
							badFiles.add(newPath);
						}
					}
				}
			}
			if(flagParamExistsAndTrue("raw")) {
				exportLines(lines);
				return markTrue();
			}

			C.say(S.format("total {}, good {}, bad {}", lines.size(), goodFiles.size(), badFiles.size()));

			if(L.bad(goodFiles)) {
				C.say("well, no good files to process");
				return markTrue();
			}

			ManyLocalFileItems many = ManyLocalFileItems.of(goodFiles);

			if(flagParamExistsAndTrue("show")) {
				exportLines(many.displayLinesByParams(rawInput));
				return markTrue();
			}

			String yourStat = stringParamOf("stat");
			if(S.good(yourStat)) {
				boolean addLink = flagParamOf("link", false);
				List<StrIntItem> statItems;
				C.say(">> get stat by {} of {} files", yourStat, goodFiles.size());
				if (S.same(yourStat, "type")) {
					statItems = many.statByType(addLink);
				} else if (S.same(yourStat, "subs")) {
					statItems = many.statBySubs(addLink);
				} else {
					throw X.of("bad stat: " + yourStat);
				}

				ManyStrNumberItems<StrIntItem> manyItems = ManyStrNumberItems.of(statItems).sort(sorter(KeyValueSorter.sorterVD));
				manyItems.forTopSome(intParamOf("top", 20));
				exportLines(manyItems.forAlignedDisplay());

				return markTrue();
			}

			String yourDup = stringParamOf("dup");
			if(S.good(yourDup)) {
				List<StrListItem> statItems;
				C.say(">> check duplicate by {} of {} files", yourDup, goodFiles.size());
				if (S.same(yourDup, "name")) {
					statItems = ManyFileItems.ofLocalFileItems(goodFiles).checkDuplicatesByName();
				} else if (S.same(yourDup, "size")) {
					statItems = ManyFileItems.ofLocalFileItems(goodFiles).checkDuplicatesBySize();
				} else if (S.same(yourDup, "sam")) {
					statItems = ManyFileItems.ofLocalFileItems(goodFiles).checkDuplicatesBySam();
				} else {
					throw X.of("bad dup: " + yourDup);
				}

				ManyStrListItems.of(statItems).printMe(yourDup + " is:");

				return markTrue();
			}

			if(flagParamExistsAndTrue("remove")) {
				C.say(">> removing {} files", goodFiles.size());
				many.remove();
				return markTrue();
			}

			String copyTo = stringParamOf("cto");
			if(S.good(copyTo)) {
				C.say(">> copying {} files", goodFiles.size());
				String whereToSave = tryRelativeOrAbsoluteOf(copyTo);
				X.makeSureIsFolder(whereToSave);
				String parentFolder = stringParamOf("parent");
				if(S.good(parentFolder)) {
					X.makeSureIsFolder(parentFolder);
				} else {
					parentFolder = pwd();
				}
				boolean preservePath = flagParamOf("pp", S.good(parentFolder));
				boolean directCopy = flagParamOf("dc", (L.sizeIs(many.getFileItems(), 1)));
				many.setParentPath(parentFolder);
				StrStrItem location = many.copyTo(whereToSave, directCopy, preservePath);
				openLocalFileWithRadarWeb(location.getFirst());

				return markTrue();
			}

			return markTrue();
		}

		return false;
	}
}