package com.jetbrains.python.facet;

import com.intellij.facet.Facet;
import com.intellij.facet.FacetType;
import com.intellij.facet.FacetTypeId;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.LibraryOrderEntry;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.OrderEntry;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar;
import com.jetbrains.python.psi.impl.PyBuiltinCache;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

/**
 * @author yole
 */
public class PythonFacet extends Facet<PythonFacetConfiguration> {
  public static final FacetTypeId<PythonFacet> ID = new FacetTypeId<PythonFacet>("python");

  @NonNls
  public static final String PYTHON_FACET_LIBRARY_NAME_SUFFIX = " interpreter library";

  public PythonFacet(@NotNull final FacetType facetType, @NotNull final Module module, final @NotNull String name, @NotNull final PythonFacetConfiguration configuration,
                     Facet underlyingFacet) {
    super(facetType, module, name, configuration, underlyingFacet);
  }

  public void updateSdkLibrary() {
    ApplicationManager.getApplication().runWriteAction(new Runnable() {
      public void run() {
        final Module module = getModule();
        final ModuleRootManager rootManager = ModuleRootManager.getInstance(module);
        final ModifiableRootModel model = rootManager.getModifiableModel();
        boolean modelChanged = false;
        // Just remove all old facet libraries except one, that is neccessary
        final Sdk sdk = getConfiguration().getSdk();
        final String name = (sdk != null) ? getFacetLibraryName(sdk.getName()) : null;
        boolean librarySeen = false;
        for (OrderEntry entry : model.getOrderEntries()) {
          if (entry instanceof LibraryOrderEntry) {
            final String libraryName = ((LibraryOrderEntry)entry).getLibraryName();
            if (name != null && name.equals(libraryName)) {
              librarySeen = true;
              continue;
            }
            if (libraryName != null && libraryName.endsWith(PYTHON_FACET_LIBRARY_NAME_SUFFIX)) {
              model.removeOrderEntry(entry);
              modelChanged = true;
            }
          }
        }
        if (!librarySeen && name != null) {
          Library library = LibraryTablesRegistrar.getInstance().getLibraryTable().getLibraryByName(name);
          if (library == null) {
            // we just create new project library
            library = PythonSdkTableListener.addLibrary(sdk);
            PyBuiltinCache.clearInstanceCache();
          }
          model.addLibraryEntry(library);
          modelChanged = true;
        }

        // !!!!!!!!!! WARNING !!!!!!!!!
        // This generates Roots Changed Event and BaseRailsFacet uses such behaviour!
        // Don't remove it without updating BaseRailsFacet behaviour!
        if (modelChanged){
          model.commit();
        } else {
          model.dispose();
        }
      }
    });
  }

  public void removeSdkLibrary() {
    ApplicationManager.getApplication().runWriteAction(new Runnable() {
      public void run()  {
        final Module module = getModule();
        final ModuleRootManager rootManager = ModuleRootManager.getInstance(module);
        final ModifiableRootModel model = rootManager.getModifiableModel();
        // Just remove all old facet libraries
        for (OrderEntry entry : model.getOrderEntries()) {
          if (entry instanceof LibraryOrderEntry) {
            final Library library = ((LibraryOrderEntry)entry).getLibrary();
            if (library != null) {
              final String libraryName = library.getName();
              if (libraryName!=null && libraryName.endsWith(PYTHON_FACET_LIBRARY_NAME_SUFFIX)) {
                model.removeOrderEntry(entry);
                //PyBuiltinCache.clearInstanceCache();
              }
            }
          }
        }
        model.commit();
      }
    });
  }

  public static String getFacetLibraryName(final String sdkName) {
    return sdkName + PYTHON_FACET_LIBRARY_NAME_SUFFIX;
  }

  public void initFacet() {
    super.initFacet();
    updateSdkLibrary();
  }
}
