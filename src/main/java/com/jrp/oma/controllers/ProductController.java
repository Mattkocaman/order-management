package com.jrp.oma.controllers;

import com.jrp.oma.entities.Category;
import com.jrp.oma.entities.Product;
import com.jrp.oma.services.CategoryService;
import com.jrp.oma.services.ProductService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;

@RestController
@RequestMapping("/products")
public class ProductController {

    private final ProductService productS;
    private final CategoryService categoryS;

    public ProductController(ProductService productS, CategoryService categoryS) {
        this.productS = productS;
        this.categoryS = categoryS;
    }

    //find  *all    *id  *name    *category id name   **price **orders
    //add *one *all
    //update *patch update
    //delete *one all
    //unique
    //sort
    //pageable


    @ResponseStatus(HttpStatus.OK)
    @GetMapping
    public List<Product> findAll() {
        return productS.findAll();
    }

    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/{id}")
    public Product findById(@PathVariable Long id) {
        if (productS.findBy(id).isPresent())
            return productS.findBy(id).get();
        return null;
    }

    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/name/{name}")
    public Product findByName(@PathVariable String name) {
        if (productS.findBy(name).isPresent())
            return productS.findBy(name).get();
        return null;
    }

    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/category/{categoryId}")
    public List<Product> findByCategoryId(@PathVariable long categoryId) {
        return productS.findAllByCategory(categoryId);
    }

    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/category/")
    public List<Product> findByCategoryName(@RequestParam String categoryName) {
        if (categoryS.findBy(categoryName).isPresent())
            return categoryS.findBy(categoryName).get().getProductList();
        return null;
    }

    @ResponseStatus(HttpStatus.OK)
    @GetMapping(value = "/price", params = {"min", "max"})
    public List<Product> findByPriceRange(@RequestParam BigDecimal min, @RequestParam BigDecimal max) {
        return productS.findByPriceBetween(min,max);
    }

    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/order/{id}")
    public List<Product> findByOrderId(@PathVariable Long id) {
        return productS.findByOrderId(id);
    }

    /**
     * This method is for adding product. It will check the category also.
     * If the category already exist via name, it will add according to it.
     * If category didn't send with product, it will add product to the uncategorized section.
     * If there is no category named before it will return HTTP bad request.
     * Update Category productList
     * Save new Product
     *
     * @param product Product has to have a name and a price.
     *                This code will look for Category also.
     * @return Saved new Product
     */
    @PostMapping("/save")
    public ResponseEntity<Product> saveAndFlush(@RequestBody Product product) {
        Category cat;
        if (product.getCategory() == null) {
            if (!categoryS.findBy("Uncategorized").isPresent()) {
                cat = new Category();
                List<Product> productList = new ArrayList<>();
                cat.setName("uncategorized");
                cat.setProductList(productList);
            } else
                cat = categoryS.findBy("Uncategorized").get();

        } else if (categoryS.findBy(product.getCategory().getName()).isPresent())
            cat = categoryS.findBy(product.getCategory().getName()).get();
        else {
            return ResponseEntity.badRequest().build();
        }

        product.setCategory(cat);
        cat.getProductList().add(product);

        return ResponseEntity.ok(productS.saveAndFlush(product));
    }

    /**
     * This method checks for duplicate product names and adds only unique values.
     *
     * @param productList Will accept product list
     * @return Will return unique added list
     */
    @ResponseStatus(HttpStatus.OK)
    @PostMapping("/saveAll")
    public List<Product> saveAll(@RequestBody List<Product> productList) {

        HashSet<String> set = new HashSet<>();
        productList.removeIf(product -> !set.add(product.getName()));

        List<Product> addedList = new ArrayList<>();
        for (Product product : productList) {
            if(saveAndFlush(product).getBody() !=null)
                addedList.add(product);
        }
        return addedList;
    }

    @PatchMapping(path = "/{id}")
    public ResponseEntity<Product> partialUpdate(@PathVariable Long id, @RequestBody Product patchProduct) {
        if (!productS.findBy(id).isPresent())
            return ResponseEntity.badRequest().build();;
        Product oldProduct = productS.findBy(id).get();

        if (patchProduct.getName() != null)
            oldProduct.setName(patchProduct.getName());

        if (patchProduct.getPrice() != null)
            oldProduct.setPrice(patchProduct.getPrice());

        if (patchProduct.getCategory() != null){
            if (!oldProduct.getCategory().getName().equals(patchProduct.getCategory().getName())) {
                if (categoryS.findBy(patchProduct.getCategory().getName()).isPresent()) {
                    oldProduct.setCategory(categoryS.findBy(patchProduct.getCategory().getName()).get());
                    return ResponseEntity.ok().body(productS.saveAndFlush(oldProduct));
                }
                return ResponseEntity.badRequest().build();
            }
        }
        return ResponseEntity.ok().body(productS.saveAndFlush(oldProduct));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Product> deleteById(@PathVariable("id") Long id) {
        if (productS.findBy(id).isPresent()) {
            productS.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.badRequest().build();
    }


    //pageable


}