package com.zain.passportmrz;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.media.Image;
import android.media.Image.Plane;
import androidx.annotation.ColorInt;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import kotlin.Metadata;
import kotlin.jvm.internal.Intrinsics;
import kotlin.ranges.ClosedRange;
import kotlin.ranges.IntRange;
import kotlin.ranges.RangesKt;
import org.jetbrains.annotations.NotNull;

@Metadata(
        mv = {1, 7, 1},
        k = 2,
        d1 = {"\u0000\"\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\b\n\u0002\b\u0004\n\u0002\u0010\u0005\n\u0000\u001a\u000e\u0010\u0002\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u0005\u001a \u0010\u0006\u001a\u00020\u00072\u0006\u0010\b\u001a\u00020\u00072\u0006\u0010\t\u001a\u00020\u00072\u0006\u0010\n\u001a\u00020\u0007H\u0003\u001a\f\u0010\u000b\u001a\u00020\u0007*\u00020\fH\u0002\"\u000e\u0010\u0000\u001a\u00020\u0001X\u0082\u0004¢\u0006\u0002\n\u0000¨\u0006\r"},
        d2 = {"CHANNEL_RANGE", "Lkotlin/ranges/IntRange;", "convertYuv420888ImageToBitmap", "Landroid/graphics/Bitmap;", "image", "Landroid/media/Image;", "yuvToRgb", "", "nY", "nU", "nV", "toIntUnsigned", "", "Passport_MRZ.app.main"}
)
public  class ImageUtils {
    private static final IntRange CHANNEL_RANGE = RangesKt.until(0, 262144);

    @NotNull
    public static final Bitmap convertYuv420888ImageToBitmap(@NotNull Image image) {
        Intrinsics.checkNotNullParameter(image, "image");
        boolean var1 = image.getFormat() == 35;
        if (!var1) {
            boolean var20 = false;
            String var21 = "Unsupported image format $(image.format)";
            throw new IllegalArgumentException(var21.toString());
        } else {
            Plane[] planes = image.getPlanes();
            Intrinsics.checkNotNullExpressionValue(planes, "planes");
            boolean $i$f$map = false;
            Plane[] $this$mapTo$iv$iv = planes;
            Collection destination$iv$iv = (Collection)(new ArrayList(planes.length));
            boolean $i$f$mapTo = false;
            int var8 = 0;

            int i;
            for(i = planes.length; var8 < i; ++var8) {
                Object item$iv$iv = $this$mapTo$iv$iv[var8];
                boolean var12 = false;
                Intrinsics.checkNotNullExpressionValue(item$iv$iv, "plane");
                ByteBuffer buffer = ((Plane) item$iv$iv).getBuffer();
                byte[] yuvBytes = new byte[buffer.capacity()];
                buffer.get(yuvBytes);
                buffer.rewind();
                destination$iv$iv.add(yuvBytes);
            }

            List yuvBytes = (List)destination$iv$iv;
            Plane var10000 = planes[0];
            Intrinsics.checkNotNullExpressionValue(planes[0], "planes[0]");
            int yRowStride = var10000.getRowStride();
            var10000 = planes[1];
            Intrinsics.checkNotNullExpressionValue(planes[1], "planes[1]");
            int uvRowStride = var10000.getRowStride();
            var10000 = planes[1];
            Intrinsics.checkNotNullExpressionValue(planes[1], "planes[1]");
            int uvPixelStride = var10000.getPixelStride();
            int width = image.getWidth();
            int height = image.getHeight();
            int[] argb8888 = new int[width * height];
            i = 0;
            int y = 0;

            for(int var11 = height; y < var11; ++y) {
                int pY = yRowStride * y;
                int uvRowStart = uvRowStride * (y >> 1);
                int x = 0;

                for(int var15 = width; x < var15; ++x) {
                    int uvOffset = (x >> 1) * uvPixelStride;
                    argb8888[i++] = yuvToRgb(toIntUnsigned(((byte[])yuvBytes.get(0))[pY + x]), toIntUnsigned(((byte[])yuvBytes.get(1))[uvRowStart + uvOffset]), toIntUnsigned(((byte[])yuvBytes.get(2))[uvRowStart + uvOffset]));
                }
            }

            Bitmap bitmap = Bitmap.createBitmap(width, height, Config.ARGB_8888);
            bitmap.setPixels(argb8888, 0, width, 0, 0, width, height);
            Intrinsics.checkNotNullExpressionValue(bitmap, "bitmap");
            return bitmap;
        }
    }

    @ColorInt
    private static final int yuvToRgb(int nY, int nU, int nV) {
        nY = nY - 16;
        nU = nU - 128;
        nV = nV - 128;
        nY = RangesKt.coerceAtLeast(nY, 0);
        int nR = 1192 * nY + 1634 * nV;
        int nG = 1192 * nY - 833 * nV - 400 * nU;
        int nB = 1192 * nY + 2066 * nU;
        nR = (int) (RangesKt.coerceIn((long) nR, (ClosedRange)CHANNEL_RANGE) >> 10 & 255);
        nG = (int) (RangesKt.coerceIn((long) nG, (ClosedRange)CHANNEL_RANGE) >> 10 & 255);
        nB = (int) (RangesKt.coerceIn((long) nB, (ClosedRange)CHANNEL_RANGE) >> 10 & 255);
        return -16777216 | nR << 16 | nG << 8 | nB;
    }

    private static final int toIntUnsigned(byte $this$toIntUnsigned) {
        return $this$toIntUnsigned & 255;
    }
}
