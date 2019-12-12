/**
 * Edit In Place JQuery plugin eip.js from https://www.jqueryscript.net/download/Edit-In-Place-Plugin-jQuery.zip
 * Modified for Onlyoffice Editor bar
 * Aug 16, 2019.
 */
(function ( $ ) { 
$.fn.editable = function (options) {
    const STYLE_EDITABLE = {
        "cursor": "pointer",
        "color": "black",
        "text-decoration-style": "none"
    };

    options          = options || {};
    options.onChange = (typeof options.onChange === 'function') ? options.onChange : function () {};

    function applyStyle(element, style) {
        Object.keys(style).forEach((property) => {
            element.style[property] = style[property];
        });
    }

    function isNewValueValid(newValue, oldValue) {
        return (newValue.trim() !== '' && oldValue !== newValue);
    }

    function setEditableElementValue(editableElement, parent, event) {
        const newValue = event.target.value;
        const oldValue = editableElement.textContent;

        if(!isNewValueValid(newValue, oldValue)){
            return;
        }

        // if the new value is valid, we set it and trigger the user's callback.
        editableElement.textContent = newValue;

        setTimeout(function(){
            options.onChange({
                parent,
                editableElement,
                event,
                oldValue,
                newValue
            });
        }, 100);
    }

    function setParent(parent, content, width) {
        parent.classList   = '';
        parent.style.width = width;
        parent.innerHTML   = content;
        parent.style.height ='4px';
    }

    function setInput(input) {
        input.focus();
        input.select();
    }

    function toInput(editableElement) {
        const parent          = editableElement.parentElement;
        const type            = (editableElement.hasAttribute('type')) ? editableElement.getAttribute('type') : 'text';
        const parentClassList = [...parent.classList];
        // Get required width from editable title
        var width = $(".editable-title").width() + "px";
        // Get the document title without extension and space
        var extension = parent.textContent.substr(parent.textContent.lastIndexOf("."));
        var documentTitle = $.trim(parent.textContent.split(extension)[0]);
        // Since we can't submit a form on "enter" whenever there is only one input in the form, a ghost one has been added.
        const wrapper = `
            <input type="text" style="display:none"/>
            <div class="form-group" style="margin: 0px;">
                <input type="${type}" class="form-control eip-editable input-sm" value="${documentTitle}"/>
            </div>
        `;

        setParent(parent, wrapper, width);
        const input = parent.querySelector('.eip-editable');
        input.style.width = '250px';
        input.style.margin = "0px";
        input.style.paddint = "1px";
        setInput(input);

        // setTimeout so we don't block the UI. Check the following link for further infos:
        // https://stackoverflow.com/questions/42266929/click-after-blur-doesnt-work
        input.addEventListener('blur', (event) => {
            setTimeout(() => {
                setEditableElementValue(editableElement, parent, event);
                toEditableElement(parent, editableElement, parentClassList);
            }, 100);
        });

        input.addEventListener('keyup', (ev) => {
            ev.preventDefault();
            switch (ev.keyCode) {
                case 13: // ENTER - apply value
                    setEditableElementValue(editableElement, parent, ev);
                    toEditableElement(parent, editableElement, parentClassList);
                    break;
                case 27: // ESC - get back to old value
                    toEditableElement(parent, editableElement, parentClassList);
                    break;
            }
        });
    }

    function toEditableElement(parent, editableElement, parentClassList) {
        parent.classList = parentClassList.join(' ');
        parent.innerHTML = "";
        parent.appendChild(editableElement);
    }

    $.each($(this), (index, editableElement) => {
        applyStyle(editableElement, STYLE_EDITABLE);
        $(editableElement).on('click', (ev) => toInput(ev.target));
    });
};
}( jQuery ));
