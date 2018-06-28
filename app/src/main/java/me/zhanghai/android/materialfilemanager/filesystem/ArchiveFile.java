/*
 * Copyright (c) 2018 Zhang Hai <Dreaming.in.Code.ZH@Gmail.com>
 * All Rights Reserved.
 */

package me.zhanghai.android.materialfilemanager.filesystem;

import android.net.Uri;
import android.os.Parcel;
import android.support.annotation.NonNull;

import org.apache.commons.compress.archivers.ArchiveException;
import org.threeten.bp.Instant;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import me.zhanghai.android.materialfilemanager.R;
import me.zhanghai.android.materialfilemanager.functional.Functional;
import me.zhanghai.android.materialfilemanager.util.CollectionUtils;

public class ArchiveFile extends BaseFile {

    public static final String SCHEME = "archive";

    private File mArchiveFile;
    private Uri mEntryPath;
    private Archive.Information mInformation;

    public ArchiveFile(Uri path) {
        super(path);

        mArchiveFile = Files.ofPath(Uri.parse(path.getSchemeSpecificPart()));
        mEntryPath = Uri.parse(path.getFragment());
    }

    public ArchiveFile(File archiveFile, Uri entryPath) {
        super(Uri.fromParts(SCHEME, archiveFile.getPath().toString(), entryPath.toString()));

        mArchiveFile = archiveFile;
        mEntryPath = entryPath;
    }

    private ArchiveFile(Uri path, Archive.Information information) {
        this(path);

        mInformation = information;
    }

    private ArchiveFile(File archiveFile, Uri entryPath, Archive.Information information) {
        this(archiveFile, entryPath);

        mInformation = information;
    }

    public File getArchiveFile() {
        return mArchiveFile;
    }

    public Uri getEntryPath() {
        return mEntryPath;
    }

    @NonNull
    @Override
    public List<File> makeFilePath() {
        List<File> path = new ArrayList<>(mArchiveFile.makeFilePath());
        CollectionUtils.pop(path);
        Uri.Builder entryPathBuilder = mEntryPath.buildUpon().path("/");
        path.add(new ArchiveFile(mArchiveFile, entryPathBuilder.build()));
        for (String entryPathSegment : mEntryPath.getPathSegments()) {
            entryPathBuilder.appendPath(entryPathSegment);
            path.add(new ArchiveFile(mArchiveFile, entryPathBuilder.build()));
        }
        return path;
    }

    @NonNull
    @Override
    public String getName() {
        List<String> segments = mEntryPath.getPathSegments();
        if (segments.isEmpty()) {
            return mArchiveFile.getName();
        }
        return CollectionUtils.last(segments);
    }

    @Override
    public void loadInformation() {

    }

    @Override
    public long getSize() {
        return mInformation.entry.getSize();
    }

    @Override
    public Instant getLastModified() {
        return Instant.ofEpochMilli(mInformation.entry.getLastModifiedDate().getTime());
    }

    @Override
    public boolean isDirectory() {
        return mInformation.entry.isDirectory();
    }

    @NonNull
    @Override
    public java.io.File makeJavaFile() {
        // TODO
        throw new UnsupportedOperationException();
    }

    @Override
    public List<File> getFileList() throws FileSystemException {
        Map<Uri, List<Archive.Information>> tree;
        try {
            tree = Archive.readTree(mArchiveFile.makeJavaFile());
        } catch (ArchiveException | IOException e) {
            throw new FileSystemException(R.string.file_list_error_archive, e);
        }
        // TODO: Handle non-existent path NPE.
        return Functional.map(tree.get(mEntryPath), information -> new ArchiveFile(mArchiveFile,
                information.path, information));
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object == null || getClass() != object.getClass()) {
            return false;
        }
        ArchiveFile that = (ArchiveFile) object;
        return Objects.equals(mPath, that.mPath)
                && Objects.equals(mArchiveFile, that.mArchiveFile)
                && Objects.equals(mEntryPath, that.mEntryPath)
                && Objects.equals(mInformation, that.mInformation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mPath, mArchiveFile, mEntryPath, mInformation);
    }


    public static final Creator<ArchiveFile> CREATOR = new Creator<ArchiveFile>() {
        @Override
        public ArchiveFile createFromParcel(Parcel source) {
            return new ArchiveFile(source);
        }
        @Override
        public ArchiveFile[] newArray(int size) {
            return new ArchiveFile[size];
        }
    };

    protected ArchiveFile(Parcel in) {
        super(in);

        mArchiveFile = in.readParcelable(File.class.getClassLoader());
        mEntryPath = in.readParcelable(Uri.class.getClassLoader());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);

        dest.writeParcelable(mArchiveFile, flags);
        dest.writeParcelable(mEntryPath, flags);
    }
}
