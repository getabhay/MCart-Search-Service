package com.nova.mcart.entity;


import com.nova.mcart.common.entity.BaseAuditEntity;
import jakarta.persistence.*;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "brand",
        indexes = {
                @Index(name = "idx_brand_is_active", columnList = "is_active")
        }
)
public class Brand extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false, unique = true)
    private String slug;

    @OneToMany(mappedBy = "brand", fetch = FetchType.LAZY)
    private List<Product> products;

    @Column(name = "is_active")
    private Boolean isActive;
}