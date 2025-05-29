package com.example.demo.dto;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DrugInfoDto {

    private String itemName;
    private String entpName;
    private String efcyQesitm;
    private String useMethodQesitm;
    private String atpnWarnQesitm;
    private String atpnQesitm;
    private String intrcQesitm;
    private String seQesitm;
    private String depositMethodQesitm;
    private String itemImage;

    public DrugInfoDto() {
    }

    public DrugInfoDto(String itemName, String entpName, String efcyQesitm, String useMethodQesitm,
                       String atpnWarnQesitm, String atpnQesitm, String intrcQesitm, String seQesitm,
                       String depositMethodQesitm, String itemImage) {
        this.itemName = itemName;
        this.entpName = entpName;
        this.efcyQesitm = efcyQesitm;
        this.useMethodQesitm = useMethodQesitm;
        this.atpnWarnQesitm = atpnWarnQesitm;
        this.atpnQesitm = atpnQesitm;
        this.intrcQesitm = intrcQesitm;
        this.seQesitm = seQesitm;
        this.depositMethodQesitm = depositMethodQesitm;
        this.itemImage = itemImage;
    }

    // Getters and Setters omitted for brevity (생략 가능)
}