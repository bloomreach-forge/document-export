# Bloomreach Development Patterns

This document captures key patterns and learnings from Bloomreach CMS development for this project.

## Dialog Pattern

### Overview

Dialogs in Bloomreach are shown through the `IDialogService` and must follow a specific structure to render correctly within the CMS framework.

### Showing a Dialog

```java
IDialogService dialogService = context.getService(IDialogService.class.getName(), IDialogService.class);
dialogService.show(new ExampleDialog(getPluginContext(), parameters));
```

**Key Point**: The dialog must be instantiated with `IPluginContext` so that `IDialogService` can properly initialize it.

### Creating a Dialog Class

Extend `AbstractDialog<T>`:

```java
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.plugin.IPluginContext;

public class ExampleDialog extends AbstractDialog<String> {
    private static final long serialVersionUID = 1L;

    public ExampleDialog(IPluginContext context, String parameter) {
        super(Model.of(""));
        // Add components directly - no intermediate panels
        add(new Label("message", "Your message here"));
    }

    @Override
    public IModel<String> getTitle() {
        return Model.of("Dialog Title");
    }

    @Override
    protected void onOk() {
        // Handle OK button click
    }
}
```

### Markup Structure

Use `wicket:extend` to allow `AbstractDialog` to provide the parent form structure:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<html xmlns:wicket="http://wicket.apache.org">
<body>
<wicket:extend>
    <p wicket:id="message"></p>
    <input type="checkbox" wicket:id="prettyPrint" />
    <a wicket:id="downloadButton">Action</a>
</wicket:extend>
</body>
</html>
```

**Critical**: Do NOT use `wicket:panel`. The `wicket:extend` tag allows AbstractDialog to provide its form wrapper.

### Component Binding

#### For Checkboxes/Form Elements

Use `AjaxCheckBox` (not `CheckBox`) to ensure real-time model synchronization:

```java
add(new AjaxCheckBox("prettyPrint", new PropertyModel<>(this, "prettyPrint")) {
    @Override
    protected void onUpdate(AjaxRequestTarget target) {
        // Optional: update other components
    }
});
```

#### For Links That Read Form State

Use `AjaxLink` - it will read the current model state when clicked:

```java
add(new AjaxLink<Void>("downloadButton") {
    @Override
    public void onClick(AjaxRequestTarget target) {
        // The 'prettyPrint' field now has the correct checkbox state
        String format = prettyPrint ? "pretty" : "compact";
        // Proceed with logic
    }
});
```

### Common Mistakes

❌ **Don't** create intermediate Panel classes:
```java
// WRONG - causes markup lookup issues
Panel contentPanel = new Panel("content");
add(contentPanel);
```

❌ **Don't** use `wicket:panel` in dialog markup:
```xml
<!-- WRONG - doesn't work with AbstractDialog's form structure -->
<wicket:panel>
    <p wicket:id="message"></p>
</wicket:panel>
```

❌ **Don't** use standard `CheckBox` for form state that's read by AjaxLink:
```java
// WRONG - checkbox state won't sync with model
add(new CheckBox("prettyPrint", new PropertyModel<>(this, "prettyPrint")));
```

## Workflow Configuration

Workflows are registered in `/hippo:configuration/hippo:workflows/` under a category name (e.g., `exportjson`), not under the generic `threepane` folder context.

Example structure:
```yaml
/hippo:configuration/hippo:workflows/exportjson:
  /exportjson-workflow:
    jcr:primaryType: frontend:workflow
    hipposys:classname: com.example.ExampleWorkflowImpl
    /frontend:renderer:
      plugin.class: com.example.ExamplePlugin
```

This is different from the folder context menu pattern documented in older examples.

## Component Communication

### IModelService Pattern

Plugins can share state through model services:

```java
public class MyPlugin extends RenderPlugin {
    @Override
    protected void onModelChanged() {
        redraw(); // Re-render when model changes
    }
}
```

### IDialogService Pattern

Plugins use `IDialogService` to show modal dialogs:

```java
IDialogService dialogService = context.getService(
    IDialogService.class.getName(),
    IDialogService.class
);
dialogService.show(new MyDialog(context, params));
```

## References

- **Bloomreach Documentation**: https://xmdocumentation.bloomreach.com/library/concepts/plugins/menu-plugin.html
- **Plugin Communication**: https://xmdocumentation.bloomreach.com/library/concepts/plugins/plugin-services.html
