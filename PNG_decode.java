import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;

class PNG_decode{
    File file = null;
    FileInputStream fis = null;

    public static void main(String args[]){
        new PNG_decode(args[0]);
    }
    public PNG_decode(String file_name){
        try{
            file = new File(file_name);
            fis = new FileInputStream(file);

            Header_read();
            Chunk_read();
            
            fis.close();
        }catch(IOException e){
            e.printStackTrace();
        }
    }
    public void Header_read() throws IOException{
        byte[] header = new byte[8];
        fis.read(header);
        if(header[0] == 0x89);
        if(header[1] == 0x50 && header[2] == 0x4e && header[3] == 0x47) System.out.println("PNG");
        if(header[4] == 0x0d && header[5] == 0x0a);
        if(header[6] == 0x1a);
        if(header[7] == 0x0a);
    }
    public void Chunk_read() throws IOException{
        int width, height; int color_depth, color_type = 0;
        int compression_method, filter_method, interlace_method;
        while(fis.available() > 0){
            int chunk_length = ByteToInteger(4);
            String chunk_type = ByteToString(4);
            //System.out.println(chunk_type);
            switch(chunk_type){
                // Critical chunk
                case "IHDR":// image header
                    width = ByteToInteger(4);
                    height = ByteToInteger(4);
                    color_depth = ByteToInteger(1);
                    color_type = ByteToInteger(1);
                    compression_method = ByteToInteger(1);
                    filter_method = ByteToInteger(1);
                    interlace_method = ByteToInteger(1);
                    System.out.println("width : " + width + ", height : " + height);
                    System.out.print("color type : ");
                    switch(color_type){
                        case 0:
                            System.out.println("grayscale");
                            break;
                        case 2:
                            System.out.println("truecolor");
                            break;
                        case 3:
                            System.out.println("indexed color");
                            break;
                        case 4:
                            System.out.println("grayscale followed by an alpha channel");
                            break;
                        case 6:
                            System.out.println("truecolor followed by an alpha channel");
                            break;
                    }
                    if(compression_method == 0) System.out.print("compression method : Deflate/Inflate");
                    if(filter_method == 0) System.out.print(" filter method : none");
                    else if(filter_method == 1) System.out.print(" filter method : ");
                    if(interlace_method == 0) System.out.println(" interlace : no interlace");
                    else if(interlace_method == 1) System.out.println(" interlace : Adam7");
                    break;
                case "PLTE":// palette
                    for(int i = 0; i < chunk_length / 3; i++){
                        int R = ByteToInteger(1);
                        int G = ByteToInteger(1);
                        int B = ByteToInteger(1);
                        System.out.println("Palette" + i + " : " + R + ", " + G + ", " + B);
                    }
                    break;
                case "IDAT":// image data
                    int CMF = ByteToInteger(1);// Compression method and flags
                    int CINFO = CMF / 16;// Compression info  CINFO = log_2 {window_size} - 8
                    int CM = CMF % 16;// Compression method
                    System.out.println("LZ77 window size : " + (1 << (CINFO + 8)));
                    if(CM == 8) System.out.println("Compression method : Deflate with a window size up to 32K");
                    else if(CM == 15);// reserved
                    int FLG = ByteToInteger(1);
                    int FLEVEL = (FLG >> 6) & 3;// compressoin level;
                    int FDICT = (FLG >> 5) & 1;// preset dictionary
                    int FCHECK = FLG & 31;// check bits for CMF and FLG
                    System.out.print("Compression level : ");
                    if(CM == 8){
                        switch(FLEVEL){
                            case 0:
                                System.out.println("fastest algorithm");
                                break;
                            case 1:
                                System.out.println("fast algorithm");
                                break;
                            case 2:
                                System.out.println("default algorithm");
                                break;
                            case 3:
                                System.out.println("maximum compression, slowest algorithm");
                                break;
                        }
                    }
                    if((256*CMF + FLG) % 31 > 0) System.out.println("error");

                    ArrayList<Integer> input = new ArrayList<Integer>();
                    ArrayList<Integer> output = new ArrayList<Integer>();
                    for(int i = 0; i < chunk_length - 6; i++){
                        int input_byte = ByteToInteger(1);
                        //for(int j = 7; j >= 0; j--){
                        for(int j = 0; j < 8; j++){
                            input.add((input_byte >> j) & 1);
                        }
                    }
                    //Inflate(input, output);
                    int adler = ByteToInteger(4);// Adler-32
                    break;
                case "IEND":// image end
                    break;

                // Ancillary chunks

                // Transparency
                case "tRNS":// transparency
                    if(color_type == 3){
                        int alpha = 0;
                        for(int i = 0; i < chunk_length; i++){
                            alpha = ByteToInteger(1);
                            System.out.println("Palette" + i + " : " + alpha);
                        }
                    }else if(color_type == 0){
                        int alpha = 0;
                        for(int i = 0; i < chunk_length / 2; i++){
                            alpha = ByteToInteger(2);
                            System.out.println("Gray level" + i + " : " + alpha);
                        }
                    }else if(color_type == 2){
                        int red_alpha = 0, green_alpha = 0, blue_alpha = 0;
                        for(int i = 0; i < chunk_length / 6; i++){
                            red_alpha = ByteToInteger(2);
                            green_alpha = ByteToInteger(2);
                            blue_alpha = ByteToInteger(2);
                            System.out.println("Palette" + i + " : " + red_alpha + ", " + green_alpha + ", " + blue_alpha);//?
                        }
                    }
                    break;
                // Color space
                case "gAMA":// Image gamma
                    // sample = light_out ^ gamma , sample, light_out in [0, 1]
                    float gamma = (float)ByteToInteger(4) / 100000;
                    System.out.println("Gamma : " + gamma);
                    break;
                case "cHRM":// Primary chromaticities
                    float white_point_x = (float)ByteToInteger(4) / 100000;
                    float white_point_y = (float)ByteToInteger(4) / 100000;
                    float red_x = (float)ByteToInteger(4) / 100000;
                    float red_y = (float)ByteToInteger(4) / 100000;
                    float green_x = (float)ByteToInteger(4) / 100000;
                    float green_y = (float)ByteToInteger(4) / 100000;
                    float blue_x = (float)ByteToInteger(4) / 100000;
                    float blue_y = (float)ByteToInteger(4) / 100000;
                    System.out.println("White Point : " + white_point_x + ", " + white_point_x);
                    System.out.println("Red : " + red_x + ", " + red_y);
                    System.out.println("Green : " + green_x + ", " + green_y);
                    System.out.println("Blue : " + blue_x + ", " + blue_y);
                    break;
                case "sRGB":// Standard RGB color space
                    int rendering_intent = ByteToInteger(1);
                    System.out.print("Rendering intent : ");
                    switch(rendering_intent){
                        case 0:
                            System.out.println("Perceptual");// good adaptation to output device gamut
                            break;
                        case 1:
                            System.out.println("Relative colorimetric");// color appearance
                            break;
                        case 2:
                            System.out.println("Saturation");// saturation
                            break;
                        case 3:
                            System.out.println("Absolute colorimetric");// colorimetry
                            break;
                    }
                    break;
                case "iCCP":{// Embedded ICC profile
                    String profile_name = ""; int null_separator = 1;
                    do{
                        profile_name += Integer.toString(null_separator);
                        null_separator = ByteToInteger(1);
                    }while(null_separator > 0);
                    int compression_method_ = ByteToInteger(1);
                    int compressed_profile = ByteToInteger(chunk_length - profile_name.length() - 2);
                    System.out.println("Profile name : " + profile_name);
                    break;
                }
                // Textual information
                case "tEXt":{// Textual data / ISO/IEC-8859-1 / Latin-1
                    String keyword = ""; int null_separator = 1;
                    do{
                        keyword += Integer.toString(null_separator);
                        null_separator = ByteToInteger(1);
                    }while(null_separator > 0);
                    String text = ByteToString(chunk_length - keyword.length() - 1);
                    System.out.println("Keyword : " + keyword + ", " + "Text : " + text);
                    break;
                }
                case "zTXt":{// Compressed textual data / Latin-1
                    String keyword = ""; int null_separator = 1; 
                    do{
                        keyword += Integer.toString(null_separator);
                        null_separator = ByteToInteger(1);
                    }while(null_separator > 0);
                    int compression_method_ = ByteToInteger(1);
                    int compressed_text = ByteToInteger(chunk_length - keyword.length() - 2);
                    System.out.println("Keyword : " + keyword + ", " + "Compressed text : " + compressed_text);
                    break;
                }
                case "iTXt":{// International textual data / UTF-8
                    String keyword = ""; int null_separator = 1; 
                    do{
                        keyword += Integer.toString(null_separator);
                        null_separator = ByteToInteger(1);
                    }while(null_separator > 0);   
                    null_separator = 1;
                    int compression_flag = ByteToInteger(1);
                    int compression_method_ = ByteToInteger(1);
                    String language_tag = "";
                    do{
                        language_tag += Integer.toString(null_separator);
                        null_separator = ByteToInteger(1);
                    }while(null_separator > 0);    
                    null_separator = 1;
                    String translated_keyword = "";
                    do{
                        translated_keyword += null_separator;//?
                        null_separator = ByteToInteger(1);
                    }while(null_separator > 0);    
                    null_separator = 1;
                    int text = ByteToInteger(chunk_length - keyword.length() - language_tag.length() - translated_keyword.length() - 5);
                    break;
                }
                // Miscellaneous information
                case "bKGD":// Background color PLTE IDAT
                    if(color_type == 3){
                        int palette_index = ByteToInteger(1);
                        System.out.println("Background : Palette" + palette_index);
                    }else if(color_type == 0 || color_type == 4){
                        int gray_level = ByteToInteger(2);
                        System.out.println("Background : " + gray_level);
                    }else if(color_type == 2 || color_type == 6){
                        int R = ByteToInteger(2);
                        int G = ByteToInteger(2);
                        int B = ByteToInteger(2);
                        System.out.println("Background : " + R + ", " + G + ", " + B);
                    }
                    break;
                case "pHYs":// Physical pixel dimensions
                    int pixels_x = ByteToInteger(4);
                    int pixels_y = ByteToInteger(4);
                    int unit_specifier = ByteToInteger(1);
                    System.out.print("pixel per unit : " + pixels_x + " x " + pixels_y + " Unit spcifier : ");
                    if(unit_specifier == 0) System.out.println("unknown");
                    else if(unit_specifier == 1) System.out.println("the meters");
                    break;
                case "sBIT":// Significant bits
                    if(color_type == 0){
                        int sbit_num = ByteToInteger(1);
                        System.out.println("Significant bits : " + sbit_num);
                    }else if(color_type == 2 || color_type == 3){
                        int red_sbit_num = ByteToInteger(1);
                        int green_sbit_num = ByteToInteger(1);
                        int blue_sbit_num = ByteToInteger(1);
                        System.out.println("Significant bits : " + red_sbit_num + ", " + green_sbit_num + ", " + blue_sbit_num);
                    }else if(color_type == 4){
                        int grayscale_sbit_num = ByteToInteger(1);
                        int alpha_sbit_num = ByteToInteger(1);
                        System.out.println("Significnat bits : " + grayscale_sbit_num + ", " + alpha_sbit_num);
                    }else if(color_type == 6){
                        int red_sbit_num = ByteToInteger(1);
                        int green_sbit_num = ByteToInteger(1);
                        int blue_sbit_num = ByteToInteger(1);
                        int alpha_sbit_num = ByteToInteger(1);
                        System.out.println("Significant bits : " + red_sbit_num + ", " + green_sbit_num + ", " + blue_sbit_num + ", " + alpha_sbit_num);
                    }
                    break;
                case "sPLT":// Suggested palette
                    String palette_name = ""; int null_terminator = 1;
                    while((null_terminator = ByteToInteger(1)) > 0){
                        palette_name += Integer.toString(null_terminator);
                    }
                    int sample_depth = ByteToInteger(1);
                    int l = 0, n = 0;
                    if(sample_depth == 8){
                        l = (chunk_length - palette_name.length() - 2) / 6;
                        n = 1;
                    }else if(sample_depth == 16){
                        l = (chunk_length - palette_name.length() - 2) / 10;
                        n = 2;
                    }
                    for(int i = 0; i < l; i++){
                        int red = ByteToInteger(n);
                        int green = ByteToInteger(n);
                        int blue = ByteToInteger(n);
                        int alpha = ByteToInteger(n);
                        int frequency = ByteToInteger(2);//?
                        System.out.println("Suggested palette " + i + " : " + red + ", " + green + ", " + blue + ", " + alpha);
                    }
                    break;
                case "hIST":// Palette histogram
                    for(int i = 0; i < chunk_length / 2; i++){
                        int hist = ByteToInteger(2);
                        System.out.println("Palette " + i + " : " + hist);
                    }
                    break;
                case "tIME":// Image last-modifcication time
                    int year = ByteToInteger(2);
                    int month = ByteToInteger(1);
                    int day = ByteToInteger(1);
                    int hour = ByteToInteger(1);
                    int minute = ByteToInteger(1);
                    int second = ByteToInteger(1);
                    System.out.println("The time of the last image modification : " + year + "/" + month + "/" + day + " " + hour + ":" + minute + ":" + second);
                    break;
                //case "acTL":// animated PNG
                //case "eXIf":// Exif metadata
                //case "fcTL":// frame control
                //case "fdAT":// frame data
                default:
                    fis.skip(fis.available());
                    break;
            }
            int crc = ByteToInteger(4);// Cyclic Redundancy Check 32
        }
    }
    public int ByteToInteger(int length) throws IOException{
        byte[] b = new byte[length];
        fis.read(b);
        int res = 0;
        for(int i = 0; i < length; i++){
            res *= 256;
            res += Byte.toUnsignedInt(b[i]);
        }
        return res;
    }
    public String ByteToString(int length) throws IOException{
        byte[] b = new byte[length];
        fis.read(b);
        return new String(b);
    }

    public void Inflate(ArrayList<Integer> input, ArrayList<Integer> output){
        class Local{
            int i;
            public int BitToInteger(int length){
                int res = 0;
                int j = i + length;
                for(; i < j; i++){
                    res = 2*res + input.get(i);
                }
                return res;
            }
            public int rBitToInteger(int length){
                int res = 0;
                for(int j = 0; j < length; j++){
                    res += (input.get(i) << j);
                    i++;
                }
                return res;
            }
        }
        
        Local local = new Local();
        local.i = 0;
        for(int i = 0; i < 100; i++) System.out.print(input.get(i));
        System.out.println();
        while(true){
            int BFINAL = local.BitToInteger(1);
            int BTYPE = local.rBitToInteger(2);System.out.println(BFINAL + " " + BTYPE);
            switch(BTYPE){
                case 0:// no compression
                    System.out.print(local.i + " ");
                    for(int i = 0; i < 16; i++) System.out.print(input.get(local.i + i));
                    System.out.print(" ");
                    for(int i = 16; i < 32; i++) System.out.print(input.get(local.i + i));
                    System.out.println();
                    while(local.i % 8 > 0) local.BitToInteger(1);
                    int LEN = local.BitToInteger(16);
                    int NLEN = local.BitToInteger(16); System.out.print(LEN + " " + NLEN + " ");
                    for(int j = 0; j < 8*LEN; j++){
                        output.add(local.BitToInteger(1));
                    }
                    break;
                case 1:// compressed with fixed Huffman codes
                    while(true){
                        int n = local.BitToInteger(7);
                        if(n <= 23) n += 256;
                        else{
                            n = 2*n + local.BitToInteger(1);
                            if(48 <= n && n <= 191) n -= 48;
                            else if(192 <= n && n <= 199) n += 88;
                            else{
                                n = 2*n + local.BitToInteger(1);
                                if(400 <= n && n <= 511) n -= 256;
                            }
                        }

                        if(n < 256){// literal
                            for(int j = 7; j >= 0; j--) output.add((n >> j) & 1);
                            System.out.print(n + " ");
                        }else if(n == 256){// end of block
                            System.out.println("end");
                            break;
                        }else if(n <= 285){// length, distance
                            int length, distance = 0;
                            if(n <= 264) length = (n - 257) + 3;
                            else if(n <= 268) length = 2*(n - 265) + local.rBitToInteger(1) + 11;
                            else if(n <= 272) length = 4*(n - 269) + local.rBitToInteger(2) + 19;
                            else if(n <= 276) length = 8*(n - 273) + local.rBitToInteger(3) + 35;
                            else if(n <= 280) length = 16*(n - 277) + local.rBitToInteger(4) + 67;
                            else if(n <= 284) length = 32*(n - 281) + local.rBitToInteger(5) + 131;
                            else length = n;

                            int head = local.BitToInteger(5);
                            if(head <= 3) distance = (head - 0) + 1;
                            else if(head <= 5) distance = 2*(head - 4) + local.rBitToInteger(1) + 5;
                            else if(head <= 7) distance = 4*(head - 6) + local.rBitToInteger(2) + 9;
                            else if(head <= 9) distance = 8*(head - 8) + local.rBitToInteger(3) + 17;
                            else if(head <= 11) distance = 16*(head - 10) + local.rBitToInteger(4) + 33;
                            else if(head <= 13) distance = 32*(head - 12) + local.rBitToInteger(5) + 65;
                            else if(head <= 15) distance = 64*(head - 14) + local.rBitToInteger(6) + 129;
                            else if(head <= 17) distance = 128*(head - 16) + local.rBitToInteger(7) + 257;
                            else if(head <= 19) distance = 256*(head - 18) + local.rBitToInteger(8) + 513;
                            else if(head <= 21) distance = 512*(head - 20) + local.rBitToInteger(9) + 1025;
                            else if(head <= 23) distance = 1024*(head - 22) + local.rBitToInteger(10) + 2049;
                            else if(head <= 25) distance = 2048*(head - 24) + local.rBitToInteger(11) + 4097;
                            else if(head <= 27) distance = 4096*(head - 26) + local.rBitToInteger(12) + 8193;
                            else if(head <= 29) distance = 8192*(head - 28) + local.rBitToInteger(13) + 16385;
                            else System.out.println("error");

                            for(int j = 0; j < distance; j++){
                                output.add(output.get(output.size() - length));
                            }
                            System.out.print(length + "," + distance + " ");
                        }
                    }
                    break;
                case 2:// compressed with dynamic Huffman codes
                    int HLIT = local.rBitToInteger(5);// literal/length - 257
                    int HDIST = local.rBitToInteger(5);// backward distance - 1
                    int HCLEN = local.rBitToInteger(4);// code length - 4
                    int[] code_length = {16, 17, 18, 0, 8, 7, 9, 6, 10, 5, 11, 4, 12, 3, 13, 2, 14, 1, 15};
                    System.out.println(HLIT + " " + HDIST + " " + HCLEN);
                    Tree[] tree1 = new Tree[19];
                    Tree[] tree2 = new Tree[HLIT + HDIST + 258];

                    // Huffman coding (Huffman coding)
                    for(int j = 0; j < (HCLEN + 4); j++){
                        tree1[code_length[j]] = new Tree(local.rBitToInteger(3), 0);
                    }
                    Huffman_coding(tree1, 8, 19);
                    for(int j = 0; j < 20; j++) System.out.print(input.get(local.i + j));
                    System.out.println();

                    // Huffman coding (data)
                    for(int i = 0; i < HLIT + HDIST + 258;){
                        int bl = 0;// bit length
                        int len = 0; int code = 0;
                        boolean flag = true;
                        while(flag){
                            len++; code = 2*code + local.BitToInteger(1);
                            for(int j = 0; j < 19; j++){
                                if(tree1[j].Len == len && tree1[j].Code == code){
                                    bl = j;
                                    flag = false;
                                }
                            }
                            if(len > 8){ System.out.println("error"); break;}
                        }
                        System.out.print(bl + " ");

                        if(bl <= 15){
                            tree2[i] = new Tree(bl, 0);
                            i++;
                        }else if(bl == 16){
                            for(int j = 0; j < local.BitToInteger(2) + 3; j++){ tree2[i] = tree2[i-1]; i++; }
                        }else if(bl == 17){
                            for(int j = 0; j < local.BitToInteger(3) + 3; j++){ tree2[i] = new Tree(0, 0); i++; }
                        }else if(bl == 18){
                            for(int j = 0; j < local.BitToInteger(7) + 11; j++){ tree2[i] = new Tree(0, 0); i++; }
                        }
                    }
                    Huffman_coding(tree2, 19, HLIT + HDIST + 258);

                    // data
                    while(true){
                        int n = 0;
                        int len = 0; int code = 0;
                        boolean flag = true;
                        while(flag){
                            len++; code = 2*code + local.BitToInteger(1);
                            for(int j = 0; j < HLIT + HDIST + 258; j++){
                                if(tree2[j].Len == len && tree2[j].Code == code){
                                    n = j;
                                    flag = false;
                                }
                            }
                        }
                        int length = 0, distance = 0;
                        if(n < 256){// literal
                            for(int j = 7; j >= 0; j--) output.add((n >> j) & 1);
                        }else if(n == 256){// end of block
                            break;
                        }else if(n < HLIT + 257){// length
                            if(n <= 264) length = (n - 257) + 3;
                            else if(n <= 268) length = 2*(n - 265) + local.BitToInteger(1) + 11;
                            else if(n <= 272) length = 4*(n - 269) + local.BitToInteger(2) + 19;
                            else if(n <= 276) length = 8*(n - 273) + local.BitToInteger(3) + 35;
                            else if(n <= 280) length = 16*(n - 277) + local.BitToInteger(4) + 67;
                            else if(n <= 284) length = 32*(n - 281) + local.BitToInteger(5) + 131;
                            else length = n;
                        }else{// distance
                            int head = n;
                            if(head <= 3) distance = (head - 0) + 1;
                            else if(head <= 5) distance = 2*(head - 4) + local.BitToInteger(1) + 5;
                            else if(head <= 7) distance = 4*(head - 6) + local.BitToInteger(2) + 9;
                            else if(head <= 9) distance = 8*(head - 8) + local.BitToInteger(3) + 17;
                            else if(head <= 11) distance = 16*(head - 10) + local.BitToInteger(4) + 33;
                            else if(head <= 13) distance = 32*(head - 12) + local.BitToInteger(5) + 65;
                            else if(head <= 15) distance = 64*(head - 14) + local.BitToInteger(6) + 129;
                            else if(head <= 17) distance = 128*(head - 16) + local.BitToInteger(7) + 257;
                            else if(head <= 19) distance = 256*(head - 18) + local.BitToInteger(8) + 513;
                            else if(head <= 21) distance = 512*(head - 20) + local.BitToInteger(9) + 1025;
                            else if(head <= 23) distance = 1024*(head - 22) + local.BitToInteger(10) + 2049;
                            else if(head <= 25) distance = 2048*(head - 24) + local.BitToInteger(11) + 4097;
                            else if(head <= 27) distance = 4096*(head - 26) + local.BitToInteger(12) + 8193;
                            else if(head <= 29) distance = 8192*(head - 28) + local.BitToInteger(13) + 16385;
                            else System.out.println("error");

                            for(int j = 0; j < distance; j++){
                                output.add(output.get(output.size() - length));
                            }
                        }
                    }
                    break;
                case 3:// reserved(error)
                    break;
            }
            if(BFINAL == 1) break;
        }
        System.out.println();
        System.out.println(input.size() + " " + local.i + " " + output.size());
    }
    class Tree{
        public int Len; int Code;
        Tree(int Len, int Code){ this.Len = Len; this.Code = Code; }
    }
    public void Huffman_coding(Tree[] tree, int max_bits, int max_code){
        int[] bl_count = new int[max_bits];
        for(int j = 0; j < max_bits; j++) bl_count[j] = 0;
        for(int j = 0; j < max_code; j++) bl_count[tree[j].Len]++;
        bl_count[0] = 0;
        int code = 0;
        int[] next_code = new int[max_bits];
        for(int bits = 1; bits < max_bits; bits++){
            code = (code + bl_count[bits-1]) << 1;
            next_code[bits] = code;
        }
        for(int n = 0; n < max_code; n++){
            int len = tree[n].Len;
            if(len != 0){
                tree[n].Code = next_code[len];
                next_code[len]++;
            }
        }
    }
    //public Filtering(){}
    //public CRC32(){}
}