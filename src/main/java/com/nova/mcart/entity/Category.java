package com.nova.mcart.entity;

import com.nova.mcart.common.entity.BaseAuditEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "category",
            indexes = {
                    @Index(name = "idx_category_slug", columnList = "slug"),
                    @Index(name = "idx_category_path", columnList = "path"),
                    @Index(name = "idx_category_parent", columnList = "parent_id"),
                    @Index(name = "idx_category_is_active", columnList = "is_active")
            }
        )
public class Category extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String name;
    @Column(nullable = false, unique = true)
    private String slug;
    private String path;

    @Column(name = "is_leaf")
    private Boolean isLeaf;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Category parent;

    @OneToMany(mappedBy = "parent", fetch = FetchType.LAZY)
    private List<Category> children;

    @OneToMany(mappedBy = "category", fetch = FetchType.LAZY)
    private List<Product> products;

    @OneToMany(mappedBy = "category", fetch = FetchType.LAZY)
    private List<CategoryAttribute> categoryAttributes;

    @Column(name = "is_active")
    private Boolean isActive;
}

