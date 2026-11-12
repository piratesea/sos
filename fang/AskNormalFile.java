package com.radar.fang;

import java.util.List;

import com.google.common.collect.Lists;
import com.radar.base.ask.KunBase;
import com.radar.base.atoz.C;
import com.radar.base.atoz.F;
import com.radar.base.atoz.O;
import com.radar.base.atoz.P;
import com.radar.base.atoz.R;
import com.radar.base.atoz.S;
import com.radar.base.atoz.X;
import com.radar.base.parts.fileSystem.Afile;
import com.radar.base.parts.fileSystem.FileSizeWithUnit;
import com.radar.base.parts.fileSystem.KunFileCreator;
import com.radar.base.parts.fileSystem.breakAndMerge.KunCatsMerger;
import com.radar.base.parts.fileSystem.breakAndMerge.KunFileBreaker;
import com.radar.base.parts.fileSystem.xfiles.faceAndFake.ClientMsDocFile;
import com.radar.base.parts.fileSystem.xfiles.faceAndFake.ClientMsPptFile;
import com.radar.base.parts.security.SecureUtil;
import com.radar.base.parts.tuple.StrObjItem;
import com.radar.base.types.KunTypes;

public class AskNormalFile extends KunBase {

    @Override
    public List<StrObjItem> getUsage() {
        List<StrObjItem> items = Lists.newArrayList();
        {
            List<String> lines = Lists.newArrayList();
            lines.add("break one big file into many pieces");
            lines.add("bySize=1M, mandatory");
            items.add(StrObjItem.of("break-file,b-file", lines));
        }
        {
            List<String> lines = Lists.newArrayList();
            lines.add("merge many pieces and save, kind of recovering");
            items.add(StrObjItem.of("merge-cats,m-cats", lines));
        }
        {
            List<String> lines = Lists.newArrayList();
            lines.add("create binary file with random bytes");
            lines.add("size=20M, wanted size");
            items.add(StrObjItem.of("create-file", lines));
        }
        {
            List<String> lines = Lists.newArrayList();
            lines.add("get MD5 value of given normal file");
            items.add(StrObjItem.of("md5", lines));
        }
        {
            List<String> lines = Lists.newArrayList();
            lines.add("get fingerprint value of given normal file");
            items.add(StrObjItem.of("sam", lines));
        }
        return items;
    }

    @Override
    public boolean ask() {

        if (leaderIs("break-file", "b-file")) {
            makeSureHasParam("normal filepath");
            String filepath = tryRelativeOrAbsoluteOf(remain);
            X.makeSureIsNormalFile(filepath);
            String bySize = stringParamOf("bySize", "5M");
            FileSizeWithUnit sizeItem = FileSizeWithUnit.parse(bySize);
            if(O.bad(sizeItem)) {
                X.showMe("bad size value from bySize={}", bySize);
            }
            long chunkSizeInByte = sizeItem.toSizeInByte();
            String maxFileSize = FileSizeWithUnit.ofBytes(Integer.MAX_VALUE).dynamicFormat();
            if(chunkSizeInByte > Integer.MAX_VALUE) {
                X.showMe("bad, too big size of {}, make it less than {}", bySize, maxFileSize);
            }
            String whereTo = P.parentOf(filepath);
            KunFileBreaker kun = KunFileBreaker.of(filepath).useTimestamp().setChunkSizeInByte((int)chunkSizeInByte).breakInto(whereTo);
            String lastPiece = kun.getLastPiece();

            openLocalFileWithRadarWeb(lastPiece);
            return markTrue();
        }

        if (leaderIs("merge-cats", "m-cats")) {
            makeSureHasParam("folder path");
            String folderPath = tryRelativeOrAbsoluteOf(remain);
            X.makeSureIsFolder(folderPath);
            String whereTo = P.parentOf(folderPath);
            KunCatsMerger kun = KunCatsMerger.of(folderPath).useTimestamp().mergeAndSave(whereTo, false);
            String finalPath = kun.getFinalFilepath();
            C.say(">> SAM:{}", Afile.of(finalPath).sampleHash());
            openLocalFileWithRadarWeb(finalPath);
            return markTrue();
        }

        if(leaderIs("create-file")) {
            String size = stringParamOf("size", true);
            String name = stringParamOf("name");
            if(S.bad(name)) {
                name = R.city().toLowerCase();
            }
            String whereTo = pwd();
            if(hasParam("w")) {
                whereTo = tryRelativeOrAbsoluteOf(stringParamOf("w"));
                X.makeSureIsFolder(whereTo);
            }
            C.time(">> creating file size of: " + size);
            String nameInfo = S.format("{}-{}", name, size);
            String finalPath = KunFileCreator.ofSize(size).setNameInfo(nameInfo).saveTo(whereTo);
            C.time(">> saved at " + finalPath);

            return markTrue();
        }

        if(leaderIs("md5")) {
            makeSureHasParam("normal filepath");
            String filepath = tryRelativeOrAbsoluteOf(remain);
            X.makeSureIsNormalFile(filepath);
            String info = S.appendWithKuohu(filepath, Afile.of(filepath).cozySize());
            C.time(">> calculating md5 of {}", info);
            String cat = SecureUtil.md5(F.of(filepath));
            C.time(cat);

            return markTrue();
        }

        if(leaderIs("sam")) {
            makeSureHasParam("normal filepath");
            String filepath = tryRelativeOrAbsoluteOf(remain);
            X.makeSureIsNormalFile(filepath);
            exportString(Afile.of(filepath).sampleHash());

            return markTrue();
        }

        if(leaderIs("extract-cats", "x-cats")) {
            makeSureHasParam("ms word filepath");
            String filepath = tryRelativeOrAbsoluteOf(remain);
            X.makeSureIsNormalFile(filepath);
            String whereToSave = stringParamOf("w");
            if(S.good(whereToSave)) {
                String temp = tryRelativeOrAbsoluteOf(whereToSave);
                X.makeSureIsFolder(temp);
            } else {
                whereToSave = pwd();
            }

            String lastPie = "";
            if(F.isPptFile(filepath)) {
                lastPie = ClientMsPptFile.of(filepath).exportImagesAndOthers(whereToSave, useTimestamp());
            } else if(F.isWordFile(filepath)) {
                lastPie = ClientMsDocFile.of(filepath).exportImagesAndOthers(whereToSave, useTimestamp());
            } else {
                X.notYetSupported(filepath);
            }
            openLocalFileWithRadarWeb(lastPie);

            return markTrue();
        }
        
        if(leaderIs("kunt")) {
//        	X.makeSureFileExists(remain);
        	String temp = KunTypes.getFileTypeByFilepath(remain); 
        	exportString("type: " + temp);
        	
        	return markTrue();
        }

        return false;
    }
}
