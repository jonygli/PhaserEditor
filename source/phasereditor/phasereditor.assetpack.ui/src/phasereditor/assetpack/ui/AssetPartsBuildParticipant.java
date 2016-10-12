package phasereditor.assetpack.ui;

import static java.lang.System.out;

import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import phasereditor.assetpack.core.AssetPackBuildParticipant;
import phasereditor.assetpack.core.AssetPackCore.PackDelta;
import phasereditor.assetpack.core.IAssetKey;
import phasereditor.assetpack.ui.editors.AssetPackEditor;
import phasereditor.assetpack.ui.views.AssetExplorer;
import phasereditor.project.core.IProjectBuildParticipant;
import phasereditor.ui.views.PreviewView;

public class AssetPartsBuildParticipant implements IProjectBuildParticipant {

	public AssetPartsBuildParticipant() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void clean(IProject project, Map<String, Object> env) {
		refreshParts();
	}

	@Override
	public void fullBuild(IProject project, Map<String, Object> env) {
		refreshParts();
	}

	private static void refreshParts() {
		Display.getDefault().asyncExec(new Runnable() {

			@Override
			public void run() {
				if (PlatformUI.getWorkbench().isClosing()) {
					return;
				}

				// explorer

				IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
				IWorkbenchPage page = window.getActivePage();
				IViewReference[] refs = page.getViewReferences();
				for (IViewReference ref : refs) {
					if (ref.getId().equals(AssetExplorer.ID)) {
						AssetExplorer view = (AssetExplorer) ref.getView(false);
						if (view != null) {
							view.refreshContent();
						}
					}
				}

				// preview windows

				for (IViewReference ref : refs) {
					if (ref.getId().equals(PreviewView.ID)) {
						PreviewView view = (PreviewView) ref.getView(false);
						Object elem = view.getPreviewElement();

						if (elem != null) {
							if (elem instanceof IAssetKey) {
								view.preview(((IAssetKey) elem).getSharedVersion());
							} else if (elem instanceof IFile) {
								if (!((IFile) elem).exists()) {
									elem = null;
								}
								view.preview(elem);
							}
						}
					}
				}

			}

		});

	}

	@Override
	public void build(IProject project, IResourceDelta delta, Map<String, Object> env) {
		Display.getDefault().asyncExec(new Runnable() {

			@SuppressWarnings("synthetic-access")
			@Override
			public void run() {
				if (PlatformUI.getWorkbench().isClosing()) {
					return;
				}

				IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
				IWorkbenchPage page = window.getActivePage();
				IViewReference[] refs = page.getViewReferences();

				buildPreviewViews(delta, refs);

				PackDelta packDelta = AssetPackBuildParticipant.getData(env);
				buildAssetsViews(packDelta, refs);

				buildAssetPackEditors(delta, page);
			}

		});
	}

	private static void buildAssetPackEditors(IResourceDelta delta, IWorkbenchPage page) {

		// all of this is shit, we should check only for resource delta and
		// renamed pack files.

		IEditorReference[] refs = page.getEditorReferences();
		for (IEditorReference ref : refs) {
			if (ref.getId().equals(AssetPackEditor.ID)) {
				AssetPackEditor editor = (AssetPackEditor) ref.getEditor(false);
				if (editor != null) {
					IFile curFile = editor.getEditorInput().getFile();
					try {
						delta.accept(d -> {
							if (d.getKind() == IResourceDelta.REMOVED && d.getResource().equals(curFile)) {
								IPath movedTo = d.getMovedToPath();
								if (movedTo == null) {
									out.println("a delete?");
									//page.closeEditor(editor, true);
								} else {
									IFile newFile = ResourcesPlugin.getWorkspace().getRoot().getFile(movedTo);
									editor.handleFileRename(newFile);
								}
							}
							return true;
						});
					} catch (CoreException e) {
						AssetPackUI.logError(e);
					}
				}
			}
		}
	}

	private static void buildAssetsViews(PackDelta packDelta, IViewReference[] refs) {
		for (IViewReference ref : refs) {
			if (ref.getId().equals(AssetExplorer.ID)) {
				AssetExplorer view = (AssetExplorer) ref.getView(false);
				if (view != null && !packDelta.isEmpty()) {
					view.refreshContent();
				}
			}
		}
	}

	private static void buildPreviewViews(IResourceDelta delta, IViewReference[] refs) {
		for (IViewReference ref : refs) {
			if (ref.getId().equals(PreviewView.ID)) {
				PreviewView view = (PreviewView) ref.getView(false);
				Object elem = view.getPreviewElement();

				if (elem != null) {
					if (elem instanceof IAssetKey) {
						updatePreview(delta, view, elem);
						continue;
					}
				}

				try {
					delta.accept(r -> {
						IResource resource = r.getResource();

						if (resource.equals(elem)) {

							if (r.getKind() == IResourceDelta.REMOVED) {
								view.preview(null);
								return false;
							}

							view.preview(elem);

							return false;
						}

						return true;
					});
				} catch (CoreException e) {
					e.printStackTrace();
				}

			}
		}
	}

	private static void updatePreview(IResourceDelta resourceDelta, PreviewView view, Object elem) {
		IAssetKey key = (IAssetKey) elem;
		IAssetKey shared = key.getSharedVersion();

		if (shared != key) {
			view.preview(shared);
			return;
		}

		if (shared.touched(resourceDelta)) {
			view.preview(elem);
		}
	}

}
