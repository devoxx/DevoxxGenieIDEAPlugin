package com.devoxx.genie.util;

import com.intellij.openapi.vfs.VirtualFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImageUtilTest {

    @Mock
    private VirtualFile virtualFile;

    // --- isImageFile tests ---

    @ParameterizedTest
    @ValueSource(strings = {"photo.jpg", "photo.jpeg", "photo.png", "photo.gif", "photo.bmp"})
    void isImageFile_supportedExtensions_returnsTrue(String fileName) {
        when(virtualFile.getName()).thenReturn(fileName);
        assertThat(ImageUtil.isImageFile(virtualFile)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"PHOTO.JPG", "PHOTO.JPEG", "PHOTO.PNG", "PHOTO.GIF", "PHOTO.BMP"})
    void isImageFile_uppercaseExtensions_returnsTrue(String fileName) {
        when(virtualFile.getName()).thenReturn(fileName);
        assertThat(ImageUtil.isImageFile(virtualFile)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"Photo.JpG", "Image.JpEg", "Icon.PnG", "Anim.GiF", "Bitmap.BmP"})
    void isImageFile_mixedCaseExtensions_returnsTrue(String fileName) {
        when(virtualFile.getName()).thenReturn(fileName);
        assertThat(ImageUtil.isImageFile(virtualFile)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"file.txt", "file.pdf", "file.svg", "file.webp", "file.tiff", "file.java", "file"})
    void isImageFile_unsupportedExtensions_returnsFalse(String fileName) {
        when(virtualFile.getName()).thenReturn(fileName);
        assertThat(ImageUtil.isImageFile(virtualFile)).isFalse();
    }

    @Test
    void isImageFile_fileWithMultipleDots_checksLastExtension() {
        when(virtualFile.getName()).thenReturn("my.file.name.png");
        assertThat(ImageUtil.isImageFile(virtualFile)).isTrue();
    }

    @Test
    void isImageFile_fileWithMultipleDots_nonImageExtension() {
        when(virtualFile.getName()).thenReturn("image.png.txt");
        assertThat(ImageUtil.isImageFile(virtualFile)).isFalse();
    }

    @Test
    void isImageFile_hiddenFile_imageExtension() {
        when(virtualFile.getName()).thenReturn(".hidden.jpg");
        assertThat(ImageUtil.isImageFile(virtualFile)).isTrue();
    }

    // --- getImageMimeType tests ---

    @Test
    void getImageMimeType_jpgFile_returnsJpegMime() {
        when(virtualFile.getName()).thenReturn("photo.jpg");
        assertThat(ImageUtil.getImageMimeType(virtualFile)).isEqualTo("image/jpeg");
    }

    @Test
    void getImageMimeType_jpegFile_returnsJpegMime() {
        when(virtualFile.getName()).thenReturn("photo.jpeg");
        assertThat(ImageUtil.getImageMimeType(virtualFile)).isEqualTo("image/jpeg");
    }

    @Test
    void getImageMimeType_pngFile_returnsPngMime() {
        when(virtualFile.getName()).thenReturn("icon.png");
        assertThat(ImageUtil.getImageMimeType(virtualFile)).isEqualTo("image/png");
    }

    @Test
    void getImageMimeType_gifFile_returnsGifMime() {
        when(virtualFile.getName()).thenReturn("animation.gif");
        assertThat(ImageUtil.getImageMimeType(virtualFile)).isEqualTo("image/gif");
    }

    @Test
    void getImageMimeType_bmpFile_returnsBmpMime() {
        when(virtualFile.getName()).thenReturn("bitmap.bmp");
        assertThat(ImageUtil.getImageMimeType(virtualFile)).isEqualTo("image/bmp");
    }

    @Test
    void getImageMimeType_unknownExtension_returnsGenericImageMime() {
        when(virtualFile.getName()).thenReturn("image.webp");
        assertThat(ImageUtil.getImageMimeType(virtualFile)).isEqualTo("image");
    }

    @Test
    void getImageMimeType_noExtension_returnsGenericImageMime() {
        when(virtualFile.getName()).thenReturn("noextension");
        assertThat(ImageUtil.getImageMimeType(virtualFile)).isEqualTo("image");
    }

    @Test
    void getImageMimeType_uppercaseJpg_returnsJpegMime() {
        when(virtualFile.getName()).thenReturn("PHOTO.JPG");
        assertThat(ImageUtil.getImageMimeType(virtualFile)).isEqualTo("image/jpeg");
    }

    @Test
    void getImageMimeType_uppercasePng_returnsPngMime() {
        when(virtualFile.getName()).thenReturn("ICON.PNG");
        assertThat(ImageUtil.getImageMimeType(virtualFile)).isEqualTo("image/png");
    }

    @Test
    void getImageMimeType_svgFile_returnsGenericImageMime() {
        when(virtualFile.getName()).thenReturn("vector.svg");
        assertThat(ImageUtil.getImageMimeType(virtualFile)).isEqualTo("image");
    }
}
