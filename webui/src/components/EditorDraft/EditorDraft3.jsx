import React, { Component } from 'react'

import {
  EditorState,
  convertToRaw,
  convertFromRaw,
  DefaultDraftBlockRenderMap
} from 'draft-js'
import {  markdownToDraft } from 'markdown-draft-js'
import  draftToMarkdown  from 'draftjs-to-markdown-imagefix';
import { Editor } from 'react-draft-wysiwyg'

import 'react-draft-wysiwyg/dist/react-draft-wysiwyg.css'
import './EditorDraft.scss'
import { isJson } from '../../functions'

import Immutable from 'immutable'

export default class EditorDraft extends Component {
  constructor (props) {
    super(props)
    this.state = {
      editorState: isJson(this.props.ideaParams.description)
        ? EditorState.createWithContent(
            convertFromRaw(JSON.parse(this.props.ideaParams.description))
          )
        : this.props.ideaParams.description ? EditorState.createWithContent(
            convertFromRaw(markdownToDraft(this.props.ideaParams.description))
          ) : EditorState.createEmpty()
    }
  }

  onEditorStateChange = editorState => {
    this.setState({
      editorState
    })

    //description: draftToMarkdown(convertToRaw(newContent)),

    const newContent = editorState.getCurrentContent()

    if (newContent) {
      this.props.setIdeaParams(prevData => {
        return {
          ...prevData,
          description: draftToMarkdown(convertToRaw(newContent)),
          descriptionPlainText: newContent.getPlainText('\u0001'),
          groupId: this.props.groupId
        }
      })
    }
  }

  blockRenderMap = Immutable.Map({
    section: {
      element: 'section'
    },
    table: {
      element: 'table'
    },
    tr: {
      element: 'tr'
    },
    td: {
      element: 'td'
    },
    th: {
      element: 'th'
    }
  })
  extendedBlockRenderMap = DefaultDraftBlockRenderMap.merge(this.blockRenderMap)

  render () {
    const { editorState } = this.state
    return (
      <Editor
        editorState={editorState}
        wrapperClassName='demo-wrapper'
        editorClassName='editorDraft'
        onEditorStateChange={this.onEditorStateChange}
        blockRenderMap={this.extendedBlockRenderMap}
        toolbar={{
          options: [
            'inline',
            'blockType',
            'list',
            'emoji',
            // 'image',
            'remove',
            'history'
          ],
          inline: {
            inDropdown: false,
            options: [
              'bold',
              'italic',
              'underline',
              'strikethrough',
              'monospace',
            ]
          },
          blockType: {
            inDropdown: true,
            options: [
              'Normal',
              'H1',
              'H2',
              'H3',
              'H4',
              'H5',
              'H6',
              'Blockquote',
              'Code'
            ]
          },

          list: {
            inDropdown: false,
            className: undefined,
            component: undefined,
            dropdownClassName: undefined,
            options: ['unordered', 'ordered']
          },

          emoji: {
            emojis: [
              '😀',
              '😁',
              '😂',
              '😃',
              '😉',
              '😋',
              '😎',
              '😍',
              '😗',
              '🤗',
              '🤔',
              '😣',
              '😫',
              '😴',
              '😌',
              '🤓',
              '😛',
              '😜',
              '😠',
              '😇',
              '😷',
              '😈',
              '👻',
              '😺',
              '😸',
              '😹',
              '😻',
              '😼',
              '😽',
              '🙀',
              '🙈',
              '🙉',
              '🙊',
              '👼',
              '👮',
              '🕵',
              '💂',
              '👳',
              '🎅',
              '👸',
              '👰',
              '👲',
              '🙍',
              '🙇',
              '🚶',
              '🏃',
              '💃',
              '⛷',
              '🏂',
              '🏌',
              '🏄',
              '🚣',
              '🏊',
              '⛹',
              '🏋',
              '🚴',
              '👫',
              '💪',
              '👈',
              '👉',
              '👉',
              '👆',
              '🖕',
              '👇',
              '🖖',
              '🤘',
              '🖐',
              '👌',
              '👍',
              '👎',
              '✊',
              '👊',
              '👏',
              '🙌',
              '🙏',
              '🐵',
              '🐶',
              '🐇',
              '🐥',
              '🐸',
              '🐌',
              '🐛',
              '🐜',
              '🐝',
              '🍉',
              '🍄',
              '🍔',
              '🍤',
              '🍨',
              '🍪',
              '🎂',
              '🍰',
              '🍾',
              '🍷',
              '🍸',
              '🍺',
              '🌍',
              '🚑',
              '⏰',
              '🌙',
              '🌝',
              '🌞',
              '⭐',
              '🌟',
              '🌠',
              '🌨',
              '🌩',
              '⛄',
              '🔥',
              '🎄',
              '🎈',
              '🎉',
              '🎊',
              '🎁',
              '🎗',
              '🏀',
              '🏈',
              '🎲',
              '🔇',
              '🔈',
              '📣',
              '🔔',
              '🎵',
              '🎷',
              '💰',
              '🖊',
              '📅',
              '✅',
              '❎',
              '💯'
            ]
          }
        }}
      />
    )
  }
}
