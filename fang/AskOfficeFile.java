package com.radar.fang;

import com.google.common.collect.Lists;
import com.radar.base.ask.KunBase;
import com.radar.base.atoz.*;
import com.radar.base.parts.fileSystem.FileTypes;
import com.radar.base.parts.fileSystem.xfiles.ExcelFileUtils;
import com.radar.base.parts.fileSystem.xfiles.KunExcelFileProcessor;
import com.radar.base.parts.fileSystem.xfiles.faceAndFake.ClientMsDocFile;
import com.radar.base.parts.fileSystem.xfiles.faceAndFake.ClientMsPptFile;
import com.radar.base.parts.stat.ManyStrAnykindItems;
import com.radar.base.parts.tuple.StrObjItem;
import com.radar.base.parts.tuple.StrStrItem;

import java.util.List;

public class AskOfficeFile extends KunBase {
    @Override
    public List<StrObjItem> getUsage() {
        List<StrObjItem> items = Lists.newArrayList();
        {
            List<String> temp = Lists.newArrayList();
            temp.add("mse C:\\path\\good.xlsx, check content of given MS excel file.");
            temp.add("cols=B,H ,cols default to be 'A'");
            temp.add("sheet=3 ,sheet default to be 0, zero-based");
            temp.add("no filepath, show the letters for excel columns, max default to be 30");
            items.add(StrObjItem.of("mse", temp));
        }
        {
            List<String> temp = Lists.newArrayList();
            temp.add("extract text and images from file such as good.docx, bad.pptx, ugly.pdf");
            items.add(StrObjItem.of("extract-cats,x-cats", temp));
        }
        return items;
    }

    @Override
    public boolean ask() {
        if(leaderIs("mse")) {
            if(S.good(remain)) {
                String excelFilepath = remain;
                X.makeSureIsNormalFile(excelFilepath);
                String cols = stringParamOf("cols", "A");
                List<String> list = S.divideBySubString(cols, ",");
                int sheetIndex = intParamOf("sheet", 0);
                D.json(KunExcelFileProcessor.of(excelFilepath).getColumnsByLettersFromAtoZ(list, sheetIndex));
            } else {
                int max = intParamOf("max", 30);
                // show column mappings ofClass number and letter
                List<StrStrItem> items = Lists.newArrayList();
                for(int ii = 1; ii <= max; ii++) {
                    items.add(StrStrItem.of(ii + "", ExcelFileUtils.convertColumnNumberToLetters(ii)));
                }
                ManyStrAnykindItems<StrStrItem> many = ManyStrAnykindItems.of(items);
                exportLines(many.forAlignedDisplay());
            }
            return markTrue();
        }

        if(leaderIs("extract-cats", "x-cats")) {
            makeSureHasParam("filepath containing text and images");
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

        return false;
    }
}
