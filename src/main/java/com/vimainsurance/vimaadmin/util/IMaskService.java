package com.vimainsurance.vimaadmin.util;

import org.springframework.web.multipart.MultipartFile;
import java.io.File;
public interface IMaskService {
    public  File maskAADHARImage(MultipartFile file);
    public  File maskPANImage(MultipartFile file);
    public  File maskAADHARPdf(MultipartFile file);
    public  File maskPANPdf(MultipartFile file);
}
