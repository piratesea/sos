package com.radar.fang;

import java.util.List;

import com.google.common.collect.Lists;
import com.radar.base.ask.AskSession;
import com.radar.base.ask.KunBase;
import com.radar.base.atoz.C;
import com.radar.base.atoz.F;
import com.radar.base.atoz.K;
import com.radar.base.atoz.S;
import com.radar.base.atoz.X;
import com.radar.base.parts.fileSystem.Afile;
import com.radar.base.parts.fileSystem.Disks;
import com.radar.base.parts.fileSystem.sync.NativePullFromLocalToLocal;
import com.radar.base.parts.fileSystem.sync.NativePushFromLocalToLocal;
import com.radar.base.parts.strings.Lines;
import com.radar.base.parts.tuple.StrObjItem;

public class AskLocalSync extends KunBase {


    @Override
    public List<StrObjItem> getUsage() {
        List<StrObjItem> items = Lists.newArrayList();
        {
            List<String> lines = Lists.newArrayList();
            lines.add("push file or folder from local to local");
            lines.add("source=/local/file/path");
            lines.add("destin=/local/folder/path");
            items.add(StrObjItem.of("push", lines));
        }
        {
            List<String> lines = Lists.newArrayList();
            lines.add("pull file or folder from local to local");
            lines.add("source=/local/file/path");
            lines.add("destin=/local/folder/path");
            items.add(StrObjItem.of("pull", lines));
        }
        {
			String temp = """
				native.defaultFileBaseForPull E:/yes/farm/pullbase/austin
				native.defaultFileBaseForPush E:/yes/farm/pushbase/austin
				""";
            items.add(StrObjItem.of("<native-config>", Lines.fromTextBlock(temp)));
        }
		{
            List<String> lines = Lists.newArrayList();
            lines.add("+pp, to preserve parents");
            lines.add("+apply, to transfer files, else check only");
            items.add(StrObjItem.of("<native>", lines));
        }
        return items;
    }

    @Override
    public boolean ask() {
        if (leaderIs("push")) {
        	C.say(">> trying to push stuff from local to local.");

            boolean checkAndApply = flagParamExistsAndTrue(K.apply);
            String localSource = tryRelativeOrAbsoluteOf(stringParamOf("source", true));
            X.makeSureFileExists(localSource);

            boolean guessPP = false;
            String localDestin = "";
            String keyForFileBase = S.format("native.defaultFileBaseForPush");
            if(hasParam("destin")) {
                localDestin = stringParamOf("destin");
            } else {
                localDestin = AskSession.stringConfigOf(keyForFileBase);
                guessPP = true;
            }
            if(S.bad(localDestin)) {
                X.showMe("missing remote destin, try [destin=/remote/folder/path] or config [{} /your/default/file/base]", keyForFileBase);
            }
            NativePushFromLocalToLocal kun = NativePushFromLocalToLocal.of(localSource, localDestin);
            kun.withParents(flagParamOf("pp", guessPP));
            if(flagParamExistsAndTrue("noChunk")) {
                kun.useChunkIfNeeded(false);
            }
            List<String> infoFolder = checkAndApply ? kun.checkAndApply() : kun.checkOnly();

            myDisplayPolicy = K.noOrderNoTotal;
            exportLines(infoFolder);
            C.say("apply? {}", checkAndApply);
            C.say("last piece: {}", kun.getLastPiece());
            if(checkAndApply) {
                if(F.isNormal(localSource)) {
                    C.say(">> SAM:{}", Afile.of(localSource).sampleHash());
                }
                if(Disks.isWindowsPath(kun.getLastPiece())) {
                    openLocalFileWithRadarWeb(kun.getLastPiece());
                }
            }

            return markTrue();
        }

        if (leaderIs("pull")) {
            C.say(">> trying to pull stuff from local to local.");

            boolean checkAndApply = flagParamExistsAndTrue(K.apply);
            String remoteSource = stringParamOf("source", true);
            String localDestin = "";
            String keyForFileBase = S.format("native.defaultFileBaseForPull");
            boolean guessPP = false;
            if(hasParam("destin")) {
                localDestin = tryRelativeOrAbsoluteOf(stringParamOf("destin"));
            } else {
            	guessPP = true;
                String fileBaseForPull = AskSession.stringConfigOf(keyForFileBase);
                if(S.good(fileBaseForPull)) {
                    X.makeSureIsFolder(fileBaseForPull);
                    localDestin = fileBaseForPull;
                }
            }
            if(S.bad(localDestin)) {
                X.showMe("missing destin, try [destin=/your/filepath] or config [{} /your/default/file/base]", keyForFileBase);
            }
            NativePullFromLocalToLocal fargo = NativePullFromLocalToLocal.of(remoteSource, localDestin);
            fargo.withParents(flagParamOf("pp", guessPP));
            List<String> infoFolder = checkAndApply ? fargo.checkAndApply() : fargo.checkOnly();

            myDisplayPolicy = K.noOrderNoTotal;
            exportLines(infoFolder);
            C.say("the longest telegram: {}", fargo.getLastPiece());
            C.say("apply? {}", checkAndApply);
            if(checkAndApply) {
                if(F.isNormal(fargo.getLastPiece())) {
                    C.say(">> SAM:{}", Afile.of(fargo.getLastPiece()).sampleHash());
                }
                openLocalFileWithRadarWeb(fargo.getLastPiece());
            }

            return markTrue();
        }

        return false;
    }

}