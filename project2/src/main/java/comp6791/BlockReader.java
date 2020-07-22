package comp6791;

import java.io.BufferedReader;
import java.io.IOException;

class BlockReader {
    private BufferedReader bufferedReader;
    private boolean isFileClose;

    BlockReader(BufferedReader bufferedReader) {
        this.bufferedReader = bufferedReader;
        isFileClose = false;
    }

    String readNext() throws IOException {
        return this.bufferedReader.readLine();
    }

    void close() throws IOException {
        this.bufferedReader.close();
        isFileClose = true;
    }

    boolean isFileClosed() {
        return !this.isFileClose;
    }
}
